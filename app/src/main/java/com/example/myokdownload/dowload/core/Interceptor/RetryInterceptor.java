package com.example.myokdownload.dowload.core.Interceptor;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.download.DownloadCache;
import com.example.myokdownload.dowload.core.download.DownloadChain;
import com.example.myokdownload.dowload.core.exception.InterruptException;
import com.example.myokdownload.dowload.core.exception.RetryException;

import java.io.IOException;

public class RetryInterceptor implements Interceptor.Connect {
    @NonNull
    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final DownloadCache cache = chain.getCache();

        while (true) {
            try {
                if (cache.isInterrupt()) {
                    throw InterruptException.SIGNAL;
                }
                return chain.processConnect();
            } catch (IOException e) {
                if (e instanceof RetryException) {
                    chain.resetConnectForRetry();
                    continue;
                }

                chain.getCache().catchException(e);
                chain.getOutputStream().catchBlockConnectException(chain.getBlockIndex());
                throw e;
            }
        }
    }
}
