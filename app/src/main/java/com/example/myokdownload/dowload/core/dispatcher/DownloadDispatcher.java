package com.example.myokdownload.dowload.core.dispatcher;

import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadDispatcher {
    volatile ExecutorService executorService;

    synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                    ThreadUtil.threadFactory("OkDownload Download", false));
        }

        return executorService;
    }
}
