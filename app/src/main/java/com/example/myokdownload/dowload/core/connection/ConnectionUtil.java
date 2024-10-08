package com.example.myokdownload.dowload.core.connection;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.log.LogUtil;
import com.example.myokdownload.BuildConfig;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectionUtil {
    // request method
    public static final String METHOD_HEAD = "HEAD";

    public static final String RANGE = "Range";
    public static final String IF_MATCH = "If-Match";
    public static final String USER_AGENT = "User-Agent";

    // response header fields.
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_RANGE = "Content-Range";
    public static final String ETAG = "Etag";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    // response header value.
    public static final String VALUE_CHUNKED = "chunked";
    public static final int CHUNKED_CONTENT_LENGTH = -1;

    // response special code.
    public static final int RANGE_NOT_SATISFIABLE = 416;

    public static boolean checkPermission(String permission) {
        final int perm = OKDownload.with().context().checkCallingOrSelfPermission(permission);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    public static long parseContentLength(@Nullable String contentLength) {
        if (contentLength == null) return CHUNKED_CONTENT_LENGTH;

        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException ignored) {
            LogUtil.d("Util", "parseContentLength failed parse for '" + contentLength + "'");
        }

        return CHUNKED_CONTENT_LENGTH;
    }

    public static boolean isNetworkNotOnWifiType(ConnectivityManager manager) {
        if (manager == null) {
            LogUtil.w("Util", "failed to get connectivity manager!");
            return true;
        }

        //noinspection MissingPermission, because we check permission accessable when invoked
        @SuppressLint("MissingPermission") final NetworkInfo info = manager.getActiveNetworkInfo();

        return info == null || info.getType() != ConnectivityManager.TYPE_WIFI;
    }

    public static boolean isNetworkAvailable(ConnectivityManager manager) {
        if (manager == null) {
            LogUtil.w("Util", "failed to get connectivity manager!");
            return true;
        }

        @SuppressLint("MissingPermission") final NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    public static void addUserRequestHeaderField(@NonNull Map<String, List<String>> userHeaderField,
                                                 @NonNull DownloadConnection connection) throws IOException {
        inspectUserHeader(userHeaderField);
        addRequestHeaderFields(userHeaderField, connection);
    }

    public static void addRequestHeaderFields(
            @NonNull Map<String, List<String>> headerFields,
            @NonNull DownloadConnection connection) {
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                connection.addHeader(key, value);
            }
        }
    }

    public static void addDefaultUserAgent(@NonNull final DownloadConnection connection) {
        final String userAgent = "OkDownload/" + BuildConfig.VERSION_NAME;
        connection.addHeader(USER_AGENT, userAgent);
    }

    public static void inspectUserHeader(@NonNull Map<String, List<String>> headerField)
            throws IOException {
        if (headerField.containsKey(IF_MATCH) || headerField.containsKey(RANGE)) {
            throw new IOException(IF_MATCH + " and " + RANGE + " only can be handle by internal!");
        }
    }

    public static long parseContentLengthFromContentRange(@Nullable String contentRange) {
        if (contentRange == null || contentRange.length() == 0) return CHUNKED_CONTENT_LENGTH;
        final String pattern = "bytes (\\d+)-(\\d+)/\\d+";
        try {
            final Pattern r = Pattern.compile(pattern);
            final Matcher m = r.matcher(contentRange);
            if (m.find()) {
                final long rangeStart = Long.parseLong(m.group(1));
                final long rangeEnd = Long.parseLong(m.group(2));
                return rangeEnd - rangeStart + 1;
            }
        } catch (Exception e) {
            LogUtil.w("Util", "parse content-length from content-range failed " + e);
        }
        return CHUNKED_CONTENT_LENGTH;
    }
}
