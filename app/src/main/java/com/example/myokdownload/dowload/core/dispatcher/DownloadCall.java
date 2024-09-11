package com.example.myokdownload.dowload.core.dispatcher;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.NamedRunnable;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.file.ProcessFileStrategy;

import java.util.ArrayList;

public class DownloadCall extends NamedRunnable {
    public final DownloadTask task;
    public final boolean asyncExecuted;
    @NonNull final ArrayList<DownloadChain> blockChainList;
    @NonNull private final DownloadStore store;
    volatile Thread currentThread;


    private DownloadCall(DownloadTask task, boolean asyncExecuted, @NonNull DownloadStore store) {
        this(task, asyncExecuted, new ArrayList<DownloadChain>(), store);
    }

    DownloadCall(DownloadTask task, boolean asyncExecuted,
                 @NonNull ArrayList<DownloadChain> runningBlockList,
                 @NonNull DownloadStore store) {
        super("download call: " + task.getId());
        this.task = task;
        this.asyncExecuted = asyncExecuted;
        this.blockChainList = runningBlockList;
        this.store = store;
    }

    public static DownloadCall create(DownloadTask task, boolean asyncExecuted,
                                      @NonNull DownloadStore store) {
        return new DownloadCall(task, asyncExecuted, store);
    }

    @Override
    protected void execute() throws InterruptedException {
        currentThread = Thread.currentThread();

        boolean retry;
        int retryCount = 0;

        final OKDownload okDownload = OKDownload.with();
        final ProcessFileStrategy fileStrategy = okDownload.processFileStrategy;

        inspectTaskStart();
    }

    @Override
    protected void interrupted(InterruptedException e) {

    }

    @Override
    protected void finished() {

    }

    private void inspectTaskStart() {
        store.onTaskStart(task.getId());
        OKDownload.with().callbackDispatcher.dispatch().taskStart(task);
    }

}
