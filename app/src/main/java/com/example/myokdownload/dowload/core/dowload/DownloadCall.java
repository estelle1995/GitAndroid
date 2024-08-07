package com.example.myokdownload.dowload.core.dowload;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.core.NamedRunnable;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadCall extends NamedRunnable implements Comparable<DownloadCall> {

    public static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(), Util.threadFactory("OKDownload Block", false));

    private static final String TAG = "DownloadCall";

    static final int MAX_COUNT_RETRY_FOR_PRECONDITION_FAILED = 1;

    public final DownloadTask task;

    volatile boolean canceled;

    public DownloadCall(DownloadTask task) {
        super("download call: " + task.getId());
        this.task = task;
    }

//    public final boolean asyncExecuted;
//
//    private DownloadCall(DownloadTask task, boolean asyncExecuted, @NonNull DownloadStore store) {
//        this(task, asyncExecuted, new ArrayList<DownloadChain>(), store);
//    }
//
//    DownloadCall(DownloadTask task, boolean asyncExecuted,
//                 @NonNull ArrayList<DownloadChain> runningBlockList,
//                 @NonNull DownloadStore store) {
//        super("download call: " + task.getId());
//        this.task = task;
//        this.asyncExecuted = asyncExecuted;
//        this.blockChainList = runningBlockList;
//        this.store = store;
//    }

    public boolean isCanceled() { return canceled; }

    public boolean equalsTask(@NonNull DownloadTask task) {
        return this.task.equals(task);
    }

    @Override
    protected void execute() throws InterruptedException {

    }

    @Override
    protected void interrupted(InterruptedException e) {

    }

    @Override
    protected void finished() {

    }

    @Override
    public int compareTo(DownloadCall downloadCall) {
        return 0;
    }
}
