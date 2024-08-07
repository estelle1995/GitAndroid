package com.example.myokdownload.dowload.core.dispatcher;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.dowload.DownloadCall;

import java.util.ArrayList;
import java.util.List;

public class DownloadDispatcher {
    private static final String TAG = "DownloadDispatcher";

    int maxParallelRunningCount = 5;

    private final List<DownloadCall> readyAsyncCalls;

    private final List<DownloadCall> runningSyncCalls;

    private final List<DownloadCall> runningAsyncCalls;

    public DownloadDispatcher() {
        this(new ArrayList<DownloadCall>(), new ArrayList<DownloadCall>(),
                new ArrayList<DownloadCall>(), new ArrayList<DownloadCall>());
    }

    DownloadDispatcher(List<DownloadCall> readyAsyncCalls,
                       List<DownloadCall> runningAsyncCalls,
                       List<DownloadCall> runningSyncCalls,
                       List<DownloadCall> finishingCalls) {
        this.readyAsyncCalls = readyAsyncCalls;
        this.runningAsyncCalls = runningAsyncCalls;
        this.runningSyncCalls = runningSyncCalls;
//        this.finishingCalls = finishingCalls;
    }

    public synchronized boolean isPending(DownloadTask task) {
        for (DownloadCall call: readyAsyncCalls) {
            if (call.isCanceled()) continue;;
            if (call.equalsTask(task)) return true;
        }
        return false;
    }

    public synchronized boolean isRunning(DownloadTask task) {
        for (DownloadCall call: runningSyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return true;
        }
        for (DownloadCall call: runningAsyncCalls) {
            if (call.isCanceled()) continue;;
            if (call.equalsTask(task)) return true;
        }
        return false;
    }
}
