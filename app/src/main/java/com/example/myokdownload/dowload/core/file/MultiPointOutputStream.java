package com.example.myokdownload.dowload.core.file;

import android.net.Uri;
import android.os.StatFs;
import android.os.SystemClock;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.breakpoint.BlockInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.cause.EndCause;
import com.example.myokdownload.dowload.core.exception.PreAllocateException;
import com.example.myokdownload.dowload.core.log.LogUtil;
import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class MultiPointOutputStream {
    private static final String TAG = "MultiPointOutputStream";
    private static final ExecutorService FILE_IO_EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>(), ThreadUtil.threadFactory("OKDownload file io", false));

    final SparseArray<DownloadOutputStream> outputStreamMap = new SparseArray<>();

    final SparseArray<AtomicLong> noSyncLengthMap = new SparseArray<>();
    final AtomicLong allNoSyncLength = new AtomicLong();
    final AtomicLong lastSyncTimestamp = new AtomicLong();
    boolean canceled = false;

    private final int flushBufferSize;
    private final int syncBufferSize;
    private final int syncBufferIntervalMills;
    private final BreakpointInfo info;
    private final DownloadTask task;
    private final DownloadStore store;
    private final boolean supportSeek;
    private final boolean isPreAllocateLength;

    volatile Future syncFuture;
    volatile Thread runSyncThread;
    final SparseArray<Thread> parkedRunBlockThreadMap = new SparseArray<>();

    @NonNull
    private final Runnable syncRunnable;
    private String path;

    IOException syncException;
    @NonNull
    ArrayList<Integer> noMoreStreamList;

    List<Integer> requireStreamBlocks;

    MultiPointOutputStream(@NonNull final DownloadTask task, @NonNull BreakpointInfo info, @NonNull DownloadStore store,
        @Nullable Runnable syncRunnable) {
        this.task = task;
        this.flushBufferSize = task.getFlushBufferSize();
        this.syncBufferSize = task.getSyncBufferSize();
        this.syncBufferIntervalMills = task.getSyncBufferIntervalMills();
        this.info = info;

        this.store = store;
        this.supportSeek = OKDownload.with().outputStreamFactory().supportSeek();
        this.isPreAllocateLength = OKDownload.with().processFileStrategy().isPreAllocateLength(task);
        this.noMoreStreamList = new ArrayList<>();
        if (syncRunnable == null) {
            this.syncRunnable = new Runnable() {
                @Override
                public void run() {
                    runSyncDelayException();
                }
            };
        } else {
            this.syncRunnable = syncRunnable;
        }

        final File file = task.getFile();
        if (file != null) this.path = file.getAbsolutePath();
    }

    public MultiPointOutputStream(@NonNull DownloadTask task,
                                  @NonNull BreakpointInfo info,
                                  @NonNull DownloadStore store) {
        this(task, info, store, null);
    }

    void runSyncDelayException() {
        try {
            runSync();
        } catch (IOException e) {
            syncException = e;
            LogUtil.w(TAG, "Sync to breakpoint-store for task[" + task.getId() + "] "
                    + "failed with cause: " + e);
        }
    }

    StreamsState state = new StreamsState();

    void runSync() throws IOException {
        LogUtil.d(TAG, "OutputStream start flush looper task[" + task.getId() + "] with "
                + "syncBufferIntervalMills[" + syncBufferIntervalMills + "] " + "syncBufferSize["
                + syncBufferSize + "]");
        runSyncThread = Thread.currentThread();

        long nextParkMills = syncBufferIntervalMills;

        flushProcess();

        while (true) {
            parkThread(nextParkMills);

            inspectStreamState(state);

            // if is no more stream, we will flush all data and quit.
            if (state.isStreamsEndOrChanged()) {
                LogUtil.d(TAG, "runSync state change isNoMoreStream[" + state.isNoMoreStream + "]"
                        + " newNoMoreStreamBlockList[" + state.newNoMoreStreamBlockList + "]");
                if (allNoSyncLength.get() > 0) {
                    flushProcess();
                }

                for (Integer blockIndex : state.newNoMoreStreamBlockList) {
                    final Thread parkedThread = parkedRunBlockThreadMap.get(blockIndex);
                    parkedRunBlockThreadMap.remove(blockIndex);
                    if (parkedThread != null) unparkThread(parkedThread);
                }

                if (state.isNoMoreStream) {
                    final int size = parkedRunBlockThreadMap.size();
                    for (int i = 0; i < size; i++) {
                        final Thread parkedThread = parkedRunBlockThreadMap.valueAt(i);
                        if (parkedThread != null) unparkThread(parkedThread);
                    }
                    parkedRunBlockThreadMap.clear();
                    break;
                } else {
                    continue;
                }
            }

            if (isNoNeedFlushForLength()) {
                nextParkMills = syncBufferIntervalMills;
                continue;
            }

            nextParkMills = getNextParkMillisecond();
            if (nextParkMills > 0) {
                continue;
            }

            flushProcess();
            nextParkMills = syncBufferIntervalMills;
        }

        LogUtil.d(TAG, "OutputStream stop flush looper task[" + task.getId() + "]");
    }

    // convenient for test.
    long getNextParkMillisecond() {
        long farToLastSyncMills = now() - lastSyncTimestamp.get();
        return syncBufferIntervalMills - farToLastSyncMills;
    }

    // convenient for test.
    long now() {
        return SystemClock.uptimeMillis();
    }

    // convenient for test.
    boolean isNoNeedFlushForLength() {
        return allNoSyncLength.get() < syncBufferSize;
    }

    void flushProcess() throws IOException {
        boolean success;
        final int size;
        synchronized (noSyncLengthMap) {
            // make sure the length of noSyncLengthMap is equal to outputStreamMap
            size = noSyncLengthMap.size();
        }

        final SparseArray<Long> increaseLengthMap = new SparseArray<>(size);

        try {
            for (int i = 0; i < size; i++) {
                final int blockIndex = outputStreamMap.keyAt(i);
                // because we get no sync length value before flush and sync,
                // so the length only possible less than or equal to the real persist
                // length.
                final long noSyncLength = noSyncLengthMap.get(blockIndex).get();
                if (noSyncLength > 0) {
                    increaseLengthMap.put(blockIndex, noSyncLength);
                    final DownloadOutputStream outputStream = outputStreamMap
                            .get(blockIndex);
                    outputStream.flushAndSync();
                }
            }
            success = true;
        } catch (IOException ex) {
            LogUtil.w(TAG, "OutputStream flush and sync data to filesystem failed " + ex);
            success = false;
        }

        if (success) {
            final int increaseLengthSize = increaseLengthMap.size();
            long allIncreaseLength = 0;
            for (int i = 0; i < increaseLengthSize; i++) {
                final int blockIndex = increaseLengthMap.keyAt(i);
                final long noSyncLength = increaseLengthMap.valueAt(i);
                store.onSyncToFilesystemSuccess(info, blockIndex, noSyncLength);
                allIncreaseLength += noSyncLength;
                noSyncLengthMap.get(blockIndex).addAndGet(-noSyncLength);
                LogUtil.d(TAG, "OutputStream sync success (" + task.getId() + ") "
                        + "block(" + blockIndex + ") " + " syncLength(" + noSyncLength + ")"
                        + " currentOffset(" + info.getBlock(blockIndex).getCurrentOffset()
                        + ")");
            }
            allNoSyncLength.addAndGet(-allIncreaseLength);
            lastSyncTimestamp.set(SystemClock.uptimeMillis());
        }
    }

    void inspectAndPersist() throws IOException {
        if (syncException != null) throw syncException;
        if (syncFuture == null) {
            synchronized (syncRunnable) {
                if (syncFuture == null) {
                    syncFuture = executeSyncRunnableAsync();
                }
            }
        }
    }

    public void catchBlockConnectException(int blockIndex) {
        noMoreStreamList.add(blockIndex);
    }

    Future executeSyncRunnableAsync() {
        return FILE_IO_EXECUTOR.submit(syncRunnable);
    }

    public synchronized void write(int blockIndex, byte[] bytes, int length) throws IOException {
        if (canceled) return;

        outputStream(blockIndex).write(bytes, 0, length);

        allNoSyncLength.addAndGet(length);
        noSyncLengthMap.get(blockIndex).addAndGet(length);
        inspectAndPersist();
    }

    final StreamsState doneState = new StreamsState();

    public void done(int blockIndex) throws IOException {
        noMoreStreamList.add(blockIndex);
        try {
            if (syncException != null) throw syncException;
            if (syncFuture != null && !syncFuture.isDone()) {
                final AtomicLong noSyncLength = noSyncLengthMap.get(blockIndex);
                if (noSyncLength != null && noSyncLength.get() > 0) {
                    inspectStreamState(doneState);
                    final boolean isNoMoreStream = doneState.isNoMoreStream;

                    ensureSync(isNoMoreStream, blockIndex);
                }
            } else {
                if (syncFuture == null) {
                    LogUtil.d(TAG, "OutputStream done but no need to ensure sync, because the "
                            + "sync job not run yet. task[" + task.getId()
                            + "] block[" + blockIndex + "]");
                } else {
                    LogUtil.d(TAG, "OutputStream done but no need to ensure sync, because the "
                            + "syncFuture.isDone[" + syncFuture.isDone() + "] task[" + task.getId()
                            + "] block[" + blockIndex + "]");
                }
            }
        } finally {
            close(blockIndex);
        }
    }

    void ensureSync(boolean isNoMoreStream, int blockIndex) {
        if (syncFuture == null || syncFuture.isDone()) return;
        if (!isNoMoreStream) {
            parkedRunBlockThreadMap.put(blockIndex, Thread.currentThread());
        }

        if (runSyncThread != null) {
            unparkThread(runSyncThread);
        } else {
            while (true) {
                if (isRunSyncThreadValid()) {
                    unparkThread(runSyncThread);
                    break;
                } else {
                    parkThread(25);
                }
            }
        }
        if (isNoMoreStream) {
            unparkThread(runSyncThread);
            try {
                syncFuture.get();
            } catch (InterruptedException ignored) {

            } catch (ExecutionException ignored) {

            }
        } else {
            parkThread();
        }
    }

    void parkThread() {
        LockSupport.park();
    }

    void parkThread(long milliseconds) {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(milliseconds));
    }

    boolean isRunSyncThreadValid() {
        return runSyncThread != null;
    }

    void unparkThread(Thread thread) {
        LockSupport.unpark(thread);
    }

    public void setRequireStreamBlocks(List<Integer> requireStreamBlocks) {
        this.requireStreamBlocks = requireStreamBlocks;
    }

    public void cancelAsync() {
        FILE_IO_EXECUTOR.execute(new Runnable() {
            @Override public void run() {
                cancel();
            }
        });
    }

    public synchronized void cancel() {
        if (requireStreamBlocks == null) return;
        if (canceled) return;
        canceled = true;
        // must ensure sync thread is finished, then can invoke 'ensureSync(true, -1)'
        // in try block, otherwise, try block will be blocked in 'ensureSync(true, -1)' and
        // codes in finally block will not be invoked
        noMoreStreamList.addAll(requireStreamBlocks);
        try {
            if (allNoSyncLength.get() <= 0) return;
            if (syncFuture != null && !syncFuture.isDone()) {
                inspectValidPath();
                OKDownload.with().processFileStrategy().getFileLock().increaseLock(path);
                try {
                    ensureSync(true, -1);
                } finally {
                    OKDownload.with().processFileStrategy().getFileLock().decreaseLock(path);
                }
            }
        } finally {
            for (Integer blockIndex: requireStreamBlocks) {
                try {
                    close(blockIndex);
                } catch (IOException e) {
                    LogUtil.d(TAG, "OutputStream close failed task[" + task.getId()
                            + "] block[" + blockIndex + "]" + e);
                }
            }
            store.onTaskEnd(task.getId(), EndCause.CANCELED, null);
        }
    }

    private void inspectValidPath() {
        if (path == null && task.getFile() != null) path = task.getFile().getAbsolutePath();
    }

    public void inspectComplete(int blockIndex) throws IOException {
        final BlockInfo blockInfo = info.getBlock(blockIndex);
        if (!(blockInfo.getCurrentOffset() == blockInfo.getContentLength())) {
            throw new IOException("The current offset on block-info isn't update correct, "
                    + blockInfo.getCurrentOffset() + " != " + blockInfo.getContentLength()
                    + " on " + blockIndex);
        }
    }

    void inspectStreamState(StreamsState state) {
        state.newNoMoreStreamBlockList.clear();

        final List<Integer> clonedList = (List<Integer>) noMoreStreamList.clone();
        final Set<Integer> uniqueBlockList = new HashSet<>(clonedList);
        final int noMoreStreamBlockCount = uniqueBlockList.size();
        if (noMoreStreamBlockCount != requireStreamBlocks.size()) {
            LogUtil.d(TAG, "task[" + task.getId() + "] current need fetching block count "
                    + requireStreamBlocks.size() + " is not equal to no more stream block count "
                    + noMoreStreamBlockCount);
            state.isNoMoreStream = false;
        } else {
            LogUtil.d(TAG, "task[" + task.getId() + "] current need fetching block count "
                    + requireStreamBlocks.size() + " is equal to no more stream block count "
                    + noMoreStreamBlockCount);
            state.isNoMoreStream = true;
        }

        final SparseArray<DownloadOutputStream> streamMap = outputStreamMap.clone();
        final int size = streamMap.size();
        for (int i = 0; i < size; i++) {
            final int blockIndex = streamMap.keyAt(i);
            if (noMoreStreamList.contains(blockIndex) && !state.noMoreStreamBlockList.contains(blockIndex)) {
                state.noMoreStreamBlockList.add(blockIndex);
                state.newNoMoreStreamBlockList.add(blockIndex);
            }
        }
    }

    synchronized void close(int blockIndex) throws IOException {
        final DownloadOutputStream outputStream = outputStreamMap.get(blockIndex);
        if (outputStream != null) {
            outputStream.close();
            outputStreamMap.remove(blockIndex);
            LogUtil.d(TAG, "OutputStream close task[" + task.getId() + "] block[" + blockIndex + "]");
        }
    }

    private volatile boolean firstOutputStream = true;

    synchronized DownloadOutputStream outputStream(int blockIndex) throws IOException {
        DownloadOutputStream outputStream = outputStreamMap.get(blockIndex);

        if (outputStream == null) {
            @NonNull final Uri uri;
            final boolean isFileScheme = Util.isUriFileScheme(task.getUri());
            if (isFileScheme) {
                final File file = task.getFile();
                if (file == null) throw new FileNotFoundException("Filename is not ready!");

                final File parentFile = task.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    throw new IOException("Create parent folder failed");
                }

                if (file.createNewFile()) {
                    LogUtil.d(TAG, "Create new file: " + file.getName());
                }

                uri = Uri.fromFile(file);
            } else {
                uri = task.getUri();
            }

            outputStream = OKDownload.with().outputStreamFactory().create(OKDownload.with().context(), uri, flushBufferSize);
            if (supportSeek) {
                final long seekPoint = info.getBlock(blockIndex).getRangeLeft();
                if (seekPoint > 0) {
                    outputStream.seek(seekPoint);
                    LogUtil.d(TAG, "Create output stream write from (" + task.getId()
                            + ") block(" + blockIndex + ") " + seekPoint);
                }
            }

            if (firstOutputStream) {
                store.markFileDirty(task.getId());
            }

            if (!info.chunked && firstOutputStream && isPreAllocateLength) {
                final long totalLength = info.getTotalLength();
                if (isFileScheme) {
                    final File file = task.getFile();
                    final long requireSpace = totalLength - file.length();
                    if (requireSpace > 0) {
                        inspectFreeSpace(new StatFs(file.getAbsolutePath()), requireSpace);
                        outputStream.setLength(totalLength);
                    }
                } else {
                    outputStream.setLength(totalLength);
                }
            }

            synchronized (noSyncLengthMap) {
                outputStreamMap.put(blockIndex, outputStream);
                noSyncLengthMap.put(blockIndex, new AtomicLong());
            }

            firstOutputStream = false;
        }
        return outputStream;
    }

    void inspectFreeSpace(StatFs statFs, long requireSpace) throws PreAllocateException {
        final long freeSpace = Util.getFreeSpaceBytes(statFs);
        if (freeSpace < requireSpace) {
            throw new PreAllocateException(requireSpace, freeSpace);
        }
    }

    static class StreamsState {
        boolean isNoMoreStream;

        List<Integer> noMoreStreamBlockList = new ArrayList<>();
        List<Integer> newNoMoreStreamBlockList = new ArrayList<>();

        boolean isStreamsEndOrChanged() {
            return isNoMoreStream || newNoMoreStreamBlockList.size() > 0;
        }
    }
}
