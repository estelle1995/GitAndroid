package com.example.myokdownload.dowload.core.Interceptor.connect;

import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.CONTENT_LENGTH;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.CONTENT_RANGE;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.IF_MATCH;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.RANGE;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.USER_AGENT;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.Interceptor.Interceptor;
import com.example.myokdownload.dowload.core.breakpoint.BlockInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.connection.ConnectionUtil;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.download.DownloadChain;
import com.example.myokdownload.dowload.core.download.DownloadStrategy;
import com.example.myokdownload.dowload.core.exception.InterruptException;
import com.example.myokdownload.dowload.core.log.LogUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderInterceptor implements Interceptor.Connect {
    private static final String TAG = "HeaderInterceptor";

    @NonNull
    @Override
    public DownloadConnection.Connected interceptConnect(DownloadChain chain) throws IOException {
        final BreakpointInfo info = chain.getInfo();
        final DownloadConnection connection = chain.getConnectionOrCreate();
        final DownloadTask task = chain.getTask();

        // add user customize header
        final Map<String, List<String>> userHeader = task.headerMapFields;
        if (userHeader != null) ConnectionUtil.addUserRequestHeaderField(userHeader, connection);
        if (userHeader == null || !userHeader.containsKey(USER_AGENT)) {
            ConnectionUtil.addDefaultUserAgent(connection);
        }

        // add range header
        final int blockIndex = chain.getBlockIndex();
        final BlockInfo blockInfo = info.getBlock(blockIndex);
        if (blockInfo == null) {
            throw new IOException("No block-info found on " + blockIndex);
        }

        String range = "bytes=" + blockInfo.getRangeLeft() + "-";
        range += blockInfo.getRangeRight();

        connection.addHeader(RANGE, range);
        LogUtil.d(TAG, "AssembleHeaderRange (" + task.getId() + ") block(" + blockIndex + ") "
                + "downloadFrom(" + blockInfo.getRangeLeft() + ") currentOffset("
                + blockInfo.getCurrentOffset() + ")");

        // add etag if exist
        final String etag = info.etag;
        if (!TextUtils.isEmpty(etag)) {
            connection.addHeader(IF_MATCH, etag);
        }

        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        OKDownload.with().callbackDispatcher.dispatch()
                .connectStart(task, blockIndex, connection.getRequestProperties());

        DownloadConnection.Connected connected = chain.processConnect();

        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }

        Map<String, List<String>> responseHeaderFields = connected.getResponseHeaderFields();
        if (responseHeaderFields == null) responseHeaderFields = new HashMap<>();

        OKDownload.with().callbackDispatcher.dispatch().connectEnd(task, blockIndex,
                connected.getResponseCode(), responseHeaderFields);

        // if precondition failed.
        final DownloadStrategy strategy = OKDownload.with().downloadStrategy;
        final DownloadStrategy.ResumeAvailableResponseCheck responseCheck =
                strategy.resumeAvailableResponseCheck(connected, blockIndex, info);
        responseCheck.inspect();

        final long contentLength;
        final String contentLengthField = connected.getResponseHeaderField(CONTENT_LENGTH);
        if (contentLengthField == null || contentLengthField.length() == 0) {
            final String contentRangeField = connected.getResponseHeaderField(CONTENT_RANGE);
            contentLength = ConnectionUtil.parseContentLengthFromContentRange(contentRangeField);
        } else {
            contentLength = ConnectionUtil.parseContentLength(contentLengthField);
        }

        chain.setResponseContentLength(contentLength);
        return connected;
    }
}
