package com.example.myokdownload.dowload.core.connection;

import androidx.annotation.Nullable;

import com.example.myokdownload.RedirectUtil;
import com.example.myokdownload.dowload.IRedirectHandler;
import com.example.myokdownload.dowload.core.Util;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class RedirectHandler implements IRedirectHandler {
    String redirectLocation;

    @Override
    public void handleRedirect(DownloadConnection originalConnection, DownloadConnection.Connected originalConnected, Map<String, List<String>> headerProperties) throws IOException {
        int responseCode = originalConnected.getResponseCode();
        int redirectCount = 0;
        final DownloadUrlConnection downloadUrlConnection = (DownloadUrlConnection) originalConnection;
        while (RedirectUtil.isRedirect(responseCode)) {
            downloadUrlConnection.release();

            if (++redirectCount > RedirectUtil.MAX_REDIRECT_TIMES) {
                throw new ProtocolException("Too many redirect requests: " + redirectCount);
            }

            redirectLocation = RedirectUtil.getRedirectedUrl(originalConnected, responseCode);
            downloadUrlConnection.url = new URL(redirectLocation);
            downloadUrlConnection.configUrlConnection();
            ConnectionUtil.addRequestHeaderFields(headerProperties,
                    downloadUrlConnection);

            downloadUrlConnection.connection.connect();
            responseCode = downloadUrlConnection.getResponseCode();
        }
    }

    @Nullable
    @Override
    public String getRedirectLocation() {
        return redirectLocation;
    }
}
