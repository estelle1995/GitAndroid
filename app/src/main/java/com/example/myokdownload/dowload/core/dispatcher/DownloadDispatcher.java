package com.example.myokdownload.dowload.core.dispatcher;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.download.DownloadCall;
import com.example.myokdownload.dowload.core.log.LogUtil;
import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
}
