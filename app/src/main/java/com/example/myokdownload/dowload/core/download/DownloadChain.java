package com.example.myokdownload.dowload.core.download;

import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.Interceptor.BreakpointInterceptor;
import com.example.myokdownload.dowload.core.Interceptor.FetchDataInterceptor;
import com.example.myokdownload.dowload.core.Interceptor.Interceptor;
import com.example.myokdownload.dowload.core.Interceptor.RetryInterceptor;
import com.example.myokdownload.dowload.core.Interceptor.connect.CallServerInterceptor;
import com.example.myokdownload.dowload.core.Interceptor.connect.HeaderInterceptor;
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

    long noCallbackIncreaseBytes;

    final AtomicBoolean finished = new AtomicBoolean(false);

    boolean isFinished() { return finished.get(); }
    volatile Thread currentThread;

    @Override
    public void run() {
        if (isFinished()) {
            throw new IllegalAccessError("The chain has been finished!");
        }
        this.currentThread = Thread.currentThread();

        try {
            start();
        } catch (IOException ignored) {

        } finally {
            finished.set(true);
            releaseConnectionAsync();
        }
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

    void start() throws IOException {
        final CallbackDispatcher dispatcher = OKDownload.with().callbackDispatcher;
        // connect chain
        final RetryInterceptor retryInterceptor = new RetryInterceptor();
        final BreakpointInterceptor breakpointInterceptor = new BreakpointInterceptor();
        connectInterceptorList.add(retryInterceptor);
        connectInterceptorList.add(breakpointInterceptor);
        connectInterceptorList.add(new HeaderInterceptor());
        connectInterceptorList.add(new CallServerInterceptor());

        connectIndex = 0;
        final DownloadConnection.Connected connected = processConnect();
        if (cache.isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        dispatcher.dispatch().fetchStart(task, blockIndex, getResponseContentLength());
        // fetch chain
        final FetchDataInterceptor fetchDataInterceptor =
                new FetchDataInterceptor(blockIndex, connected.getInputStream(),
                        getOutputStream(), task);
        fetchInterceptorList.add(retryInterceptor);
        fetchInterceptorList.add(breakpointInterceptor);
        fetchInterceptorList.add(fetchDataInterceptor);

        fetchIndex = 0;
        final long totalFetchedBytes = processFetch();
        dispatcher.dispatch().fetchEnd(task, blockIndex, totalFetchedBytes);
    }

    static DownloadChain createChain(int blockIndex, DownloadTask task, @NonNull BreakpointInfo info, @NonNull DownloadCache cache, @NonNull DownloadStore store) {
        return new DownloadChain(blockIndex, task, info, cache, store);
    }

    public DownloadConnection.Connected processConnect() throws IOException {
        if (cache.isInterrupt()) throw InterruptException.SIGNAL;
        return connectInterceptorList.get(connectIndex++).interceptConnect(this);
    }

    public long getResponseContentLength() {
        return responseContentLength;
    }

    public long processFetch() throws IOException {
        if (cache.isInterrupt()) throw InterruptException.SIGNAL;
        return fetchInterceptorList.get(fetchIndex++).interceptFetch(this);
    }

    public long loopFetch() throws IOException {
        if (fetchIndex == fetchInterceptorList.size()) {
            // last one is fetch data interceptor
            fetchIndex--;
        }
        return processFetch();
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

    public void flushNoCallbackIncreaseBytes() {
        if (noCallbackIncreaseBytes == 0) return;

        callbackDispatcher.dispatch().fetchProgress(task, blockIndex, noCallbackIncreaseBytes);
        noCallbackIncreaseBytes = 0;
    }

    public void resetConnectForRetry() {
        connectIndex = 1;
        releaseConnection();
    }

    public void increaseCallbackBytes(long increaseBytes) {
        this.noCallbackIncreaseBytes += increaseBytes;
    }

    public synchronized void releaseConnection() {
        if (connection != null) {
            connection.release();
            LogUtil.d(TAG, "release connection " + connection + " task[" + task.getId()
                    + "] block[" + blockIndex + "]");
        }
        connection = null;
    }

    void releaseConnectionAsync() {
        EXECUTOR.execute(releaseConnectionRunnable);
    }

    private final Runnable releaseConnectionRunnable = new Runnable() {
        @Override public void run() {
            releaseConnection();
        }
    };
}
