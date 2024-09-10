package com.example.myokdownload.dowload.core.connection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.IRedirectHandler;
import com.example.myokdownload.dowload.core.log.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

public class DownloadUrlConnection implements DownloadConnection.Connected, DownloadConnection{
    URLConnection connection;
    Configuration configuration;
    URL url;
    IRedirectHandler redirectHandler;

    private static final String TAG = "DownloadUrlConnection";

    DownloadUrlConnection(URLConnection connection) {
        this(connection, new RedirectHandler());
    }

    DownloadUrlConnection(URLConnection connection, IRedirectHandler redirectHandler) {
        this.connection = connection;
        this.url = connection.getURL();
        this.redirectHandler = redirectHandler;
    }

    public DownloadUrlConnection(String originUrl, Configuration configuration) throws IOException {
        this(new URL(originUrl), configuration);
    }

    public DownloadUrlConnection(URL url, Configuration configuration) throws IOException {
        this(url, configuration, new RedirectHandler());
    }

    public DownloadUrlConnection(
            URL url,
            Configuration configuration,
            IRedirectHandler redirectHandler) throws IOException {
        this.configuration = configuration;
        this.url = url;
        this.redirectHandler = redirectHandler;
        configUrlConnection();
    }

    public DownloadUrlConnection(String originUrl) throws IOException {
        this(originUrl, null);
    }

    void configUrlConnection() throws IOException {
        LogUtil.d(TAG, "config connection for" + url);
        if (configuration != null && configuration.proxy != null) {
            connection = url.openConnection(configuration.proxy);
        } else {
            connection = url.openConnection();
        }
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
        }
        if (configuration != null) {
            if (configuration.readTimeout != null) {
                connection.setReadTimeout(configuration.readTimeout);
            }

            if (configuration.connectTimeout != null) {
                connection.setConnectTimeout(configuration.connectTimeout);
            }
        }
    }


    @Override
    public void addHeader(String name, String value) {
        connection.addRequestProperty(name, value);
    }

    @Override
    public boolean setRequestMethod(@NonNull String method) throws ProtocolException {
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection) connection).setRequestMethod(method);
            return true;
        }
        return false;
    }

    @Override
    public Connected execute() throws IOException {
        final Map<String, List<String>> headerProperties = getRequestProperties();
        connection.connect();
        redirectHandler.handleRedirect(this, this, headerProperties);
        return null;
    }

    @Override
    public void release() {
        try {
            final InputStream inputStream = connection.getInputStream();
            if (inputStream != null) inputStream.close();
        } catch (IOException ignored) {

        }
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return connection.getRequestProperties();
    }

    @Override
    public String getRequestProperty(String key) {
        return connection.getRequestProperty(key);
    }

    @Override
    public int getResponseCode() throws IOException {
        if (connection instanceof HttpURLConnection) {
            return ((HttpURLConnection) connection).getResponseCode();
        }

        return DownloadConnection.NO_RESPONSE_CODE;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    @Nullable
    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        return connection.getHeaderFields();
    }

    @Nullable
    @Override
    public String getResponseHeaderField(String name) {
        return connection.getHeaderField(name);
    }

    @Override
    public String getRedirectLocation() {
        return redirectHandler.getRedirectLocation();
    }
}
