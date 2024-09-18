package com.example.myokdownload.dowload;

import com.example.myokdownload.dowload.core.dispatcher.CallbackDispatcher;
import com.example.myokdownload.dowload.core.file.DownloadOutputStream;
import com.example.myokdownload.dowload.core.file.ProcessFileStrategy;

public class OKDownload {
    private static volatile OKDownload instance;
    public DownloadMonitor monitor;
    public ProcessFileStrategy processFileStrategy;
    public CallbackDispatcher callbackDispatcher;
    public DownloadOutputStream.Factory outputStreamFactory;

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
