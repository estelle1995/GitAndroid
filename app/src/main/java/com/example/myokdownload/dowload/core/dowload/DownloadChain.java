package com.example.myokdownload.dowload.core.dowload;

import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadChain implements Runnable {
    final AtomicBoolean finished = new AtomicBoolean(false);
    volatile Thread currentThread;

    @Override
    public void run() {

    }

    public void cancel() {
        if (finished.get() || this.currentThread == null) return;

        currentThread.interrupt();
    }
}
