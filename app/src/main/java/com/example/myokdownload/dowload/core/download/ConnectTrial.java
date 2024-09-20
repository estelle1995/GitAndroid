package com.example.myokdownload.dowload.core.download;

import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.ACCEPT_RANGES;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.CHUNKED_CONTENT_LENGTH;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.CONTENT_DISPOSITION;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.CONTENT_LENGTH;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.CONTENT_RANGE;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.ETAG;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.IF_MATCH;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.METHOD_HEAD;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.RANGE;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.TRANSFER_ENCODING;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.VALUE_CHUNKED;

import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadListener;
import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.connection.ConnectionUtil;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.exception.DownloadSecurityException;
import com.example.myokdownload.dowload.core.log.LogUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectTrial {
    private static final String TAG = "ConnectTrial";
    @NonNull
    private final DownloadTask task;
    @NonNull
    private final BreakpointInfo info;

    private boolean acceptRange;
    @IntRange(from = CHUNKED_CONTENT_LENGTH) private long instanceLength;
    @Nullable
    private String responseEtag;
    @Nullable
    private String responseFilename;
    private int responseCode;

    public ConnectTrial(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
        this.task = task;
        this.info = info;
    }

    public void executeTrial() throws IOException {
        OKDownload.with().downloadStrategy.inspectNetworkOnWifi(task);
        OKDownload.with().downloadStrategy.inspectNetworkAvailable();

        DownloadConnection connection = OKDownload.with().connectionFactory.create(task.getUrl());
        boolean isNeedTrialHeadMethod;
        try {
            if (!TextUtils.isEmpty(info.etag)) {
                connection.addHeader(IF_MATCH, info.etag);
            }
            connection.addHeader(RANGE, "bytes=0-0");
            final Map<String, List<String>> userHeader = task.headerMapFields;
            if (userHeader != null) ConnectionUtil.addRequestHeaderFields(userHeader, connection);

            final DownloadListener listener = OKDownload.with().callbackDispatcher.dispatch();
            final Map<String, List<String>> requestProperties = connection.getRequestProperties();
            listener.connectTrialStart(task, requestProperties);

            final DownloadConnection.Connected connected = connection.execute();
            task.redirectLocation = connected.getRedirectLocation();
            LogUtil.d(TAG, "task[" + task.getId() + "] redirect location: "
                    + task.redirectLocation);

            this.responseCode = connected.getResponseCode();
            this.acceptRange = isAcceptRange(connected);
            this.instanceLength = findInstanceLength(connected);
            this.responseEtag = findEtag(connected);
            this.responseFilename = findFilename(connected);
            Map<String, List<String>> responseHeader = connected.getResponseHeaderFields();
            if (responseHeader == null) responseHeader = new HashMap<>();
            listener.connectTrialEnd(task, responseCode, responseHeader);
            isNeedTrialHeadMethod = isNeedTrialHeadMethodForInstanceLength(instanceLength, connected);
        } finally {
            connection.release();
        }
        if (isNeedTrialHeadMethod) {
            trialHeadMethodForInstanceLength();
        }
    }

    public long getInstanceLength() {
        return this.instanceLength;
    }

    public boolean isAcceptRange() {
        return this.acceptRange;
    }

    public boolean isChunked() {
        return this.instanceLength == CHUNKED_CONTENT_LENGTH;
    }

    @Nullable public String getResponseEtag() {
        return responseEtag;
    }

    @Nullable public String getResponseFilename() {
        return responseFilename;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public boolean isEtagOverdue() {
        return info.etag != null && !info.etag.equals(responseEtag);
    }

    void trialHeadMethodForInstanceLength() throws IOException {
        final DownloadConnection connection = OKDownload.with().connectionFactory.create(task.getUrl());
        final DownloadListener listener = OKDownload.with().callbackDispatcher.dispatch();

        try {
            connection.setRequestMethod(METHOD_HEAD);
            final Map<String, List<String>> userHeader = task.headerMapFields;
            if (userHeader != null)  ConnectionUtil.addUserRequestHeaderField(userHeader, connection);

            listener.connectTrialStart(task, connection.getRequestProperties());
            final DownloadConnection.Connected connectedForContentLength = connection.execute();
            listener.connectTrialEnd(task, connectedForContentLength.getResponseCode(),
                    connectedForContentLength.getResponseHeaderFields());

            this.instanceLength = ConnectionUtil.parseContentLength(
                    connectedForContentLength.getResponseHeaderField(CONTENT_LENGTH));
        } finally {
            connection.release();
        }
    }

    boolean isNeedTrialHeadMethodForInstanceLength(long oldInstanceLength, @NonNull DownloadConnection.Connected connected) {
        if (oldInstanceLength != CHUNKED_CONTENT_LENGTH) return false;

        final String contentRange = connected.getResponseHeaderField(CONTENT_RANGE);
        if (contentRange != null && contentRange.length() > 0) {
            // because of the Content-Range can certain the result is right, so pass.
            return false;
        }

        final boolean isChunked = parseTransferEncoding(
                connected.getResponseHeaderField(TRANSFER_ENCODING));
        if (isChunked) {
            // because of the Transfer-Encoding can certain the result is right, so pass.
            return false;
        }

        final String contentLengthField = connected.getResponseHeaderField(CONTENT_LENGTH);
        if (contentLengthField == null || contentLengthField.length() <= 0) {
            // because of the response header isn't contain the Content-Length so the HEAD method
            // request is useless, because we plan to get the right instance-length on the
            // Content-Length field through the response header of non 0-0 Range HEAD method request
            return false;
        }

        // because of the response header contain Content-Length, but because of we using Range: 0-0
        // so we the Content-Length is always 1 now, we can't use it, so we try to use HEAD method
        // request just for get the certain instance-length.
        return true;
    }

    @Nullable private static String findFilename(DownloadConnection.Connected connected)
            throws IOException {
        return parseContentDisposition(connected.getResponseHeaderField(CONTENT_DISPOSITION));
    }

    private static final Pattern CONTENT_DISPOSITION_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");
    // no note
    private static final Pattern CONTENT_DISPOSITION_NON_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*(.*)");

    @Nullable private static String parseContentDisposition(String contentDisposition)
            throws IOException {
        if (contentDisposition == null) {
            return null;
        }

        try {
            String fileName = null;
            Matcher m = CONTENT_DISPOSITION_QUOTED_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                fileName = m.group(1);
            } else  {
                m = CONTENT_DISPOSITION_NON_QUOTED_PATTERN.matcher(contentDisposition);
                if (m.find()) {
                    fileName = m.group(1);
                }
            }

            if (fileName != null && fileName.contains("../")) {
                throw new DownloadSecurityException("The filename [" + fileName + "] from"
                        + " the response is not allowable, because it contains '../', which "
                        + "can raise the directory traversal vulnerability");
            }

            return fileName;
        } catch (IllegalStateException ex) {
            // This function is defined as returning null when it can't parse the header
        }
        return null;
    }

    @Nullable private static String findEtag(DownloadConnection.Connected connected) {
        return connected.getResponseHeaderField(ETAG);
    }

    private static long findInstanceLength(DownloadConnection.Connected connected) {
        final long instanceLength = parseContentRangeFoInstanceLength(CONTENT_RANGE);
        if (instanceLength != CHUNKED_CONTENT_LENGTH) return instanceLength;

        final boolean isChunked = parseTransferEncoding(connected.getResponseHeaderField(TRANSFER_ENCODING));
        if (!isChunked) {
            LogUtil.w(TAG, "Transfer-Encoding isn't chunked but there is no "
                    + "valid instance length found either!");
        }

        return CHUNKED_CONTENT_LENGTH;
    }

    private static boolean isAcceptRange(@NonNull DownloadConnection.Connected connected)
            throws IOException {
        if (connected.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) return true;

        final String acceptRanges = connected.getResponseHeaderField(ACCEPT_RANGES);
        return "bytes".equals(acceptRanges);
    }

    private static boolean parseTransferEncoding(@Nullable String transferEncoding) {
        return transferEncoding != null && transferEncoding.equals(VALUE_CHUNKED);
    }

    private static long parseContentRangeFoInstanceLength(@Nullable String contentRange) {
        if (contentRange == null) return CHUNKED_CONTENT_LENGTH;

        final String[] session = contentRange.split("/");
        if (session.length >= 2) {
            try {
                return Long.parseLong(session[1]);
            } catch (NumberFormatException e) {
                LogUtil.w(TAG, "parse instance length failed with " + contentRange);
            }
        }

        return -1;
    }
}
