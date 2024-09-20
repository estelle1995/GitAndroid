package com.example.myokdownload.dowload;

import android.content.Context;

import com.example.myokdownload.dowload.core.breakpoint.BreakpointStore;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.dispatcher.CallbackDispatcher;
import com.example.myokdownload.dowload.core.dispatcher.DownloadDispatcher;
import com.example.myokdownload.dowload.core.download.DownloadStrategy;
import com.example.myokdownload.dowload.core.file.DownloadOutputStream;
import com.example.myokdownload.dowload.core.file.ProcessFileStrategy;

public class OKDownload {
    private static volatile OKDownload instance;
    public DownloadMonitor monitor;
    public ProcessFileStrategy processFileStrategy;
    public CallbackDispatcher callbackDispatcher;
    public DownloadOutputStream.Factory outputStreamFactory;
    public DownloadDispatcher downloadDispatcher;
    public DownloadStrategy downloadStrategy;
    public Context context;
    public DownloadConnection.Factory connectionFactory;
    public BreakpointStore breakpointStore;

    private OKDownload() {
    }

    public static OKDownload with() {
        if (instance != null) return instance;
        synchronized (OKDownload.class) {
            if (instance != null) return instance;
            instance = new OKDownload();
            return instance;
        }
    }

}
