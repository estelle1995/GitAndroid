package com.example.myokdownload.dowload.core.dispatcher;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.StatusUtil;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.cause.EndCause;
import com.example.myokdownload.dowload.core.download.DownloadCall;
import com.example.myokdownload.dowload.core.log.LogUtil;
import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadDispatcher {
    private static final String TAG = "DownloadDispatcher";

    int maxParallelRunningCount = 5;

    private final List<DownloadCall> readyAsyncCalls;
    private final List<DownloadCall> runningAsyncCalls;
    private final List<DownloadCall> runningSyncCalls;

    private final List<DownloadCall> finishingCalls;

    private final AtomicInteger flyingCanceledAsyncCallCount = new AtomicInteger();

    private @Nullable volatile ExecutorService executorService;

    private final AtomicInteger skipProceedCallCount = new AtomicInteger();

    private DownloadStore store;

    public DownloadDispatcher() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    DownloadDispatcher(List<DownloadCall> readyAsyncCalls, List<DownloadCall> runningAsyncCalls,
                       List<DownloadCall> runningSyncCalls, List<DownloadCall> finishingCalls) {
        this.readyAsyncCalls = readyAsyncCalls;
        this.runningSyncCalls = runningSyncCalls;
        this.runningAsyncCalls = runningAsyncCalls;
        this.finishingCalls = finishingCalls;
    }

    public void setDownloadStore(@NonNull DownloadStore store) {
        this.store = store;
    }

    synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                    ThreadUtil.threadFactory("OkDownload Download", false));
        }

        return executorService;
    }

    private synchronized void enqueueIgnorePriority(DownloadTask task) {
        final DownloadCall call = DownloadCall.create(task, true, store);
        if (runningAsyncSize() < maxParallelRunningCount) {
            runningAsyncCalls.add(call);
            getExecutorService().execute(call);
        } else {
            readyAsyncCalls.add(call);
        }
    }

    private int runningAsyncSize() {
        return runningAsyncCalls.size() - flyingCanceledAsyncCallCount.get();
    }

    public synchronized void flyingCanceled(DownloadCall call) {
        LogUtil.d(TAG, "flying canceled: " + call.task.getId());
        if (call.asyncExecuted) flyingCanceledAsyncCallCount.incrementAndGet();
    }

    public synchronized boolean isFileConflictAfterRun(@NonNull DownloadTask task) {
        LogUtil.d(TAG, "is file conflict after run: " + task.getId());
        final File file = task.getFile();
        if (file == null) return false;

        for (DownloadCall syncCall: runningSyncCalls) {
            if (syncCall.isCanceled() || syncCall.task == task) continue;
            final File otherFile = syncCall.task.getFile();
            if (otherFile != null && file.equals(otherFile)) {
                return true;
            }
        }

        for (DownloadCall asyncCall : runningAsyncCalls) {
            if (asyncCall.isCanceled() || asyncCall.task == task) continue;

            final File otherFile = asyncCall.task.getFile();
            if (otherFile != null && file.equals(otherFile)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public synchronized DownloadTask findSameTask(DownloadTask task) {
        LogUtil.d(TAG, "findSameTask: " + task.getId());
        for (DownloadCall call : readyAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return call.task;
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return call.task;
        }

        for (DownloadCall call : runningSyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return call.task;
        }

        return null;
    }

    public synchronized void finish(DownloadCall call) {
        final boolean asyncExecuted = call.asyncExecuted;
        final Collection<DownloadCall> calls;
        if (finishingCalls.contains(call)) {
            calls = finishingCalls;
        } else if (asyncExecuted) {
            calls = runningAsyncCalls;
        } else {
            calls = runningSyncCalls;
        }
        if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
        if (asyncExecuted && call.isCanceled()) flyingCanceledAsyncCallCount.decrementAndGet();
        if (asyncExecuted) processCalls();
    }

    private synchronized void processCalls() {
        if (skipProceedCallCount.get() > 0) return;
        if (runningAsyncSize() >= maxParallelRunningCount) return;

        for (Iterator<DownloadCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            DownloadCall call = i.next();

            i.remove();

            final DownloadTask task = call.task;
            if (isFileConflictAfterRun(task)) {
                OKDownload.with().callbackDispatcher.dispatch().taskEnd(task, EndCause.FILE_BUSY,
                        null);
                continue;
            }

            runningAsyncCalls.add(call);
            getExecutorService().execute(call);

            if (runningAsyncSize() >= maxParallelRunningCount) return;
        }
    }

    public synchronized boolean isRunning(DownloadTask task) {
        LogUtil.d(TAG, "isRunning: " + task.getId());
        for (DownloadCall call : runningSyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) {
                return true;
            }
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) {
                return true;
            }
        }

        return false;
    }

    public synchronized boolean isPending(DownloadTask task) {
        LogUtil.d(TAG, "isPending: " + task.getId());
        for (DownloadCall call : readyAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return true;
        }

        return false;
    }
}
