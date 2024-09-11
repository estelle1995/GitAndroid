package com.example.myokdownload.dowload.thread;

import androidx.annotation.NonNull;

import java.util.concurrent.ThreadFactory;

public class ThreadUtil {
    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable runnable) {
                final Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }
}
