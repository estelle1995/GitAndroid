package com.example.myokdownload.dowload;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointStore;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.dispatcher.CallbackDispatcher;
import com.example.myokdownload.dowload.core.dispatcher.DownloadDispatcher;
import com.example.myokdownload.dowload.core.download.DownloadStrategy;
import com.example.myokdownload.dowload.core.file.DownloadOutputStream;
import com.example.myokdownload.dowload.core.file.DownloadUriOutputStream;
import com.example.myokdownload.dowload.core.file.ProcessFileStrategy;
import com.example.myokdownload.dowload.core.log.LogUtil;

public class OKDownload {
    @SuppressLint("StaticFieldLeak") static volatile OKDownload singleton;

    private final DownloadDispatcher downloadDispatcher;
    private final CallbackDispatcher callbackDispatcher;
    private final BreakpointStore breakpointStore;
    private final DownloadConnection.Factory connectionFactory;
    private final DownloadOutputStream.Factory outputStreamFactory;
    private final ProcessFileStrategy processFileStrategy;
    private final DownloadStrategy downloadStrategy;

    private final Context context;

    @Nullable
    DownloadMonitor monitor;

    OKDownload(Context context, DownloadDispatcher downloadDispatcher,
               CallbackDispatcher callbackDispatcher, DownloadStore store,
               DownloadConnection.Factory connectionFactory,
               DownloadOutputStream.Factory outputStreamFactory,
               ProcessFileStrategy processFileStrategy, DownloadStrategy downloadStrategy) {
        this.context = context;
        this.downloadDispatcher = downloadDispatcher;
        this.callbackDispatcher = callbackDispatcher;
        this.breakpointStore = store;
        this.connectionFactory = connectionFactory;
        this.outputStreamFactory = outputStreamFactory;
        this.processFileStrategy = processFileStrategy;
        this.downloadStrategy = downloadStrategy;

        this.downloadDispatcher.setDownloadStore(Util.createRemitDatabase(store));
    }

    public DownloadDispatcher downloadDispatcher() { return downloadDispatcher; }

    public CallbackDispatcher callbackDispatcher() { return callbackDispatcher; }

    public BreakpointStore breakpointStore() { return breakpointStore; }

    public DownloadConnection.Factory connectionFactory() { return connectionFactory; }

    public DownloadOutputStream.Factory outputStreamFactory() { return outputStreamFactory; }

    public ProcessFileStrategy processFileStrategy() { return processFileStrategy; }

    public DownloadStrategy downloadStrategy() { return downloadStrategy; }

    public Context context() { return this.context; }

    public void setMonitor(@Nullable DownloadMonitor monitor) {
        this.monitor = monitor;
    }

    @Nullable public DownloadMonitor getMonitor() {
        return monitor;
    }

    public static OKDownload with() {
        if (singleton == null) {
            synchronized (OKDownload.class) {
                if (singleton == null) {
                    if (OKDownloadProvider.context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    singleton = new Builder(OKDownloadProvider.context).build();
                }
            }
        }
        return singleton;
    }

    public static void setSingletonInstance(@NonNull OKDownload okDownload) {
        if (singleton != null) {
            throw new IllegalArgumentException(("OkDownload must be null."));
        }

        synchronized (OKDownload.class) {
            if (singleton != null) {
                throw new IllegalArgumentException(("OkDownload must be null."));
            }
            singleton = okDownload;
        }
    }

    public static class Builder {
        private DownloadDispatcher downloadDispatcher;
        private CallbackDispatcher callbackDispatcher;
        private DownloadStore downloadStore;
        private DownloadConnection.Factory connectionFactory;
        private ProcessFileStrategy processFileStrategy;
        private DownloadStrategy downloadStrategy;
        private DownloadOutputStream.Factory outputStreamFactory;
        private DownloadMonitor monitor;
        private final Context context;

        public Builder(@NonNull Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder downloadDispatcher(DownloadDispatcher downloadDispatcher) {
            this.downloadDispatcher = downloadDispatcher;
            return this;
        }

        public Builder callbackDispatcher(CallbackDispatcher callbackDispatcher) {
            this.callbackDispatcher = callbackDispatcher;
            return this;
        }

        public Builder downloadStore(DownloadStore downloadStore) {
            this.downloadStore = downloadStore;
            return this;
        }

        public Builder connectionFactory(DownloadConnection.Factory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public Builder outputStreamFactory(DownloadOutputStream.Factory outputStreamFactory) {
            this.outputStreamFactory = outputStreamFactory;
            return this;
        }

        public Builder processFileStrategy(ProcessFileStrategy processFileStrategy) {
            this.processFileStrategy = processFileStrategy;
            return this;
        }

        public Builder downloadStrategy(DownloadStrategy downloadStrategy) {
            this.downloadStrategy = downloadStrategy;
            return this;
        }

        public Builder monitor(DownloadMonitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public OKDownload build() {
            if (downloadDispatcher == null) {
                downloadDispatcher = new DownloadDispatcher();
            }

            if (callbackDispatcher == null) {
                callbackDispatcher = new CallbackDispatcher();
            }

            if (downloadStore == null) {
                downloadStore = Util.createDefaultDatabase(context);
            }

            if (connectionFactory == null) {
                connectionFactory = Util.createDefaultConnectionFactory();
            }

            if (outputStreamFactory == null) {
                outputStreamFactory = new DownloadUriOutputStream.Factory();
            }

            if (processFileStrategy == null) {
                processFileStrategy = new ProcessFileStrategy();
            }

            if (downloadStrategy == null) {
                downloadStrategy = new DownloadStrategy();
            }

            OKDownload okDownload = new OKDownload(context, downloadDispatcher, callbackDispatcher,
                    downloadStore, connectionFactory, outputStreamFactory, processFileStrategy,
                    downloadStrategy);

            okDownload.setMonitor(monitor);

            LogUtil.d("OkDownload", "downloadStore[" + downloadStore + "] connectionFactory["
                    + connectionFactory);
            return okDownload;
        }
    }
}
