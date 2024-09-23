package com.example.myokdownload.dowload.core.file;

import android.os.SystemClock;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.log.LogUtil;
import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        this.flushBufferSize = task.flushBufferSize;
        this.syncBufferSize = task.syncBufferSize;
        this.syncBufferIntervalMills = task.syncBufferIntervalMills;
        this.info = info;

        this.store = store;
        this.supportSeek = OKDownload.with().outputStreamFactory.supportSeek();
        this.isPreAllocateLength = OKDownload.with().processFileStrategy.isPreAllocateLength(task);
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

    public void cancelAsync() {
        FILE_IO_EXECUTOR.execute(new Runnable() {
            @Override public void run() {
                cancel();
            }
        });
    }

    public synchronized void cancel() {

    }
}
