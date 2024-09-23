package com.example.myokdownload.dowload.core.Interceptor.connect;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.Interceptor.Interceptor;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.download.DownloadChain;

import java.io.IOException;

public class CallServerInterceptor implements Interceptor.Connect {
    @NonNull
    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        OKDownload.with().downloadStrategy.inspectNetworkOnWifi(chain.getTask());
        OKDownload.with().downloadStrategy.inspectNetworkAvailable();

        return chain.getConnectionOrCreate().execute();
    }
}
