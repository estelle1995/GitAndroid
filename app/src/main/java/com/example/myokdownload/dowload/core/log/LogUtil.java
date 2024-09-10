package com.example.myokdownload.dowload.core.log;

import android.util.Log;

import androidx.annotation.Nullable;

public class LogUtil {
    private static Logger logger = new EmptyLogger();

    public interface Logger {
        void e(String tag, String msg, Exception e);

        void w(String tag, String msg);

        void d(String tag, String msg);

        void i(String tag, String msg);
    }

    public static class EmptyLogger implements Logger {
        @Override public void e(String tag, String msg, Exception e) { }

        @Override public void w(String tag, String msg) { }

        @Override public void d(String tag, String msg) { }

        @Override public void i(String tag, String msg) { }
    }

    public static void enableConsoleLog() {
        logger = null;
    }

    /**
     * Set the logger which using on okdownload.
     * default one is {@link EmptyLogger}.
     *
     * @param l if provide logger is {@code null} we will using {@link Log} as default.
     */
    public static void setLogger(@Nullable Logger l) {
        logger = l;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void e(String tag, String msg, Exception e) {
        if (logger != null) {
            logger.e(tag, msg, e);
            return;
        }

        Log.e(tag, msg, e);
    }

    public static void w(String tag, String msg) {
        if (logger != null) {
            logger.w(tag, msg);
            return;
        }

        Log.w(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (logger != null) {
            logger.d(tag, msg);
            return;
        }

        Log.d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (logger != null) {
            logger.i(tag, msg);
            return;
        }

        Log.i(tag, msg);
    }

}
