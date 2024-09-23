package com.example.myokdownload.dowload.core.download;

import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.Interceptor.Interceptor;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.dispatcher.CallbackDispatcher;
import com.example.myokdownload.dowload.core.exception.InterruptException;
import com.example.myokdownload.dowload.core.file.MultiPointOutputStream;
import com.example.myokdownload.dowload.core.log.LogUtil;
import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadChain implements Runnable {
    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
            60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
            ThreadUtil.threadFactory("OkDownload Cancel Block", false));

    private static final String TAG = "DownloadChain";

    private final int blockIndex;

    @NonNull private final DownloadTask task;
    @NonNull private final BreakpointInfo info;
    @NonNull private final DownloadCache cache;

    final List<Interceptor.Connect> connectInterceptorList = new ArrayList<>();
    final List<Interceptor.Fetch> fetchInterceptorList = new ArrayList<>();
    int connectIndex = 0;
    int fetchIndex = 0;

    private final CallbackDispatcher callbackDispatcher;

    private long responseContentLength;
    @NonNull private final DownloadStore store;

    private volatile DownloadConnection connection;

    final AtomicBoolean finished = new AtomicBoolean(false);

    boolean isFinished() { return finished.get(); }
    volatile Thread currentThread;

    @Override
    public void run() {

    }

    public void cancel() {

    }
    private DownloadChain(int blockIndex, @NonNull DownloadTask task, @NonNull BreakpointInfo info,
                          @NonNull DownloadCache cache, @NonNull DownloadStore store) {
        this.blockIndex = blockIndex;
        this.task = task;
        this.cache = cache;
        this.info = info;
        this.store = store;
        this.callbackDispatcher = OKDownload.with().callbackDispatcher;
    }

    static DownloadChain createChain(int blockIndex, DownloadTask task, @NonNull BreakpointInfo info, @NonNull DownloadCache cache, @NonNull DownloadStore store) {
        return new DownloadChain(blockIndex, task, info, cache, store);
    }

    public DownloadConnection.Connected processConnect() throws IOException {
        if (cache.isInterrupt()) throw InterruptException.SIGNAL;
        return connectInterceptorList.get(connectIndex++).interceptConnect(this);
    }

    @NonNull public synchronized DownloadConnection getConnectionOrCreate() throws IOException {
        if (cache.isInterrupt()) throw InterruptException.SIGNAL;

        if (connection == null) {
            final String url;
            final String redirectLocation = cache.getRedirectLocation();
            if (redirectLocation != null) {
                url = redirectLocation;
            } else {
                url = info.url;
            }

            LogUtil.d(TAG, "create connection on url: " + url);

            connection = OKDownload.with().connectionFactory.create(url);
        }
        return connection;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    @NonNull public DownloadCache getCache() {
        return cache;
    }

    @NonNull public BreakpointInfo getInfo() {
        return this.info;
    }

    @NonNull public DownloadTask getTask() {
        return task;
    }

    public void setResponseContentLength(long responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    @NonNull public DownloadStore getDownloadStore() {
        return store;
    }

    public MultiPointOutputStream getOutputStream() {
        return this.cache.getOutputStream();
    }

    public void resetConnectForRetry() {
        connectIndex = 1;
        releaseConnection();
    }

    public synchronized void releaseConnection() {
        if (connection != null) {
            connection.release();
            LogUtil.d(TAG, "release connection " + connection + " task[" + task.getId()
                    + "] block[" + blockIndex + "]");
        }
        connection = null;
    }
}
