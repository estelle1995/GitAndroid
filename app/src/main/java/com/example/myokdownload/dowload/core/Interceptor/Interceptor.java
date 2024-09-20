package com.example.myokdownload.dowload.core.Interceptor;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.download.DownloadChain;

import java.io.IOException;

public interface Interceptor {
    interface Connect {
        @NonNull
        DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException;
    }

    interface Fetch {
        long interceptFetch(DownloadChain chain) throws IOException;
    }
}
