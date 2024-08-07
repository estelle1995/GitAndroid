package com.example.myokdownload.dowload;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.core.breakpoint.BreakpointStore;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.dispatcher.DownloadDispatcher;

public class OkDownload {
    @SuppressLint("StaticFieldLeak") static volatile OkDownload singleton;

    private final Context context;

    private final BreakpointStore breakpointStore;

    private final DownloadDispatcher downloadDispatcher;

    OkDownload(Context context, DownloadDispatcher downloadDispatcher, DownloadStore store) {
        this.context = context;
        this.breakpointStore = store;
        this.downloadDispatcher = downloadDispatcher;
    }

    public Context context() { return this.context; }

    public BreakpointStore breakpointStore() { return breakpointStore; }

    public DownloadDispatcher downloadDispatcher() { return downloadDispatcher; }

    public static OkDownload with() {
        if (singleton == null) {
            synchronized (OkDownload.class) {
                if (singleton == null) {
                    if (OkDownloadProvider.context == null) {
                        throw new IllegalStateException("context == null");
                    }
                    singleton = new Builder(OkDownloadProvider.context).build();
                }
            }
        }
        return singleton;
    }

    public static class Builder {
        private DownloadDispatcher downloadDispatcher;
        private final Context context;
        private DownloadStore downloadStore;
        public Builder(@NonNull Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder downloadDispatcher(DownloadDispatcher downloadDispatcher) {
            this.downloadDispatcher = downloadDispatcher;
            return this;
        }

        public Builder downloadStore(DownloadStore downloadStore) {
            this.downloadStore = downloadStore;
            return this;
        }

        public OkDownload build() {
            if (downloadStore == null) {
                downloadStore = Util.createDefaultDatabase(context);
            }
            if (downloadDispatcher == null) {
                downloadDispatcher = new DownloadDispatcher();
            }
            OkDownload okDownload = new OkDownload(context, downloadDispatcher, downloadStore);
            return okDownload;
        }
    }
}
