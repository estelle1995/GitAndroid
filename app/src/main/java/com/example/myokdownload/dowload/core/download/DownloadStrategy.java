package com.example.myokdownload.dowload.core.download;

import static com.example.myokdownload.dowload.core.cause.ResumeFailedCause.RESPONSE_CREATED_RANGE_NOT_FROM_0;
import static com.example.myokdownload.dowload.core.cause.ResumeFailedCause.RESPONSE_ETAG_CHANGED;
import static com.example.myokdownload.dowload.core.cause.ResumeFailedCause.RESPONSE_PRECONDITION_FAILED;
import static com.example.myokdownload.dowload.core.cause.ResumeFailedCause.RESPONSE_RESET_RANGE_NOT_FROM_0;
import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.ETAG;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.breakpoint.BlockInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointStore;
import com.example.myokdownload.dowload.core.cause.ResumeFailedCause;
import com.example.myokdownload.dowload.core.connection.ConnectionUtil;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.exception.NetworkPolicyException;
import com.example.myokdownload.dowload.core.exception.ResumeFailedException;
import com.example.myokdownload.dowload.core.exception.ServerCanceledException;
import com.example.myokdownload.dowload.core.log.LogUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadStrategy {
    private static final String TAG = "DownloadStrategy";

    // 1 connection: [0, 1MB)
    private static final long ONE_CONNECTION_UPPER_LIMIT = 1024 * 1024; // 1MiB
    // 2 connection: [1MB, 5MB)
    private static final long TWO_CONNECTION_UPPER_LIMIT = 5 * 1024 * 1024; // 5MiB
    // 3 connection: [5MB, 50MB)
    private static final long THREE_CONNECTION_UPPER_LIMIT = 50 * 1024 * 1024; // 50MiB
    // 4 connection: [50MB, 100MB)
    private static final long FOUR_CONNECTION_UPPER_LIMIT = 100 * 1024 * 1024; // 100MiB

    Boolean isHasAccessNetworkStatePermission = null;
    private ConnectivityManager manager = null;

    public boolean isUseMultiBlock(final boolean isAcceptRange) {
        if (!OKDownload.with().outputStreamFactory.supportSeek()) return false;
        return isAcceptRange;
    }

    @Nullable public ResumeFailedCause getPreconditionFailedCause(int responseCode, boolean isAlreadyProceed, @NonNull BreakpointInfo info, @Nullable String responseEtag) {
        final String localEtag = info.etag;
        if (responseCode == HttpURLConnection.HTTP_PRECON_FAILED) {
            return RESPONSE_PRECONDITION_FAILED;
        }

        if (!TextUtils.isEmpty(localEtag) && !TextUtils.isEmpty(responseEtag) && !responseEtag
                .equals(localEtag)) {
            // etag changed.
            // also etag changed is relate to HTTP_PRECON_FAILED
            return RESPONSE_ETAG_CHANGED;
        }

        if (responseCode == HttpURLConnection.HTTP_CREATED && isAlreadyProceed) {
            return RESPONSE_CREATED_RANGE_NOT_FROM_0;
        }

        if (responseCode == HttpURLConnection.HTTP_RESET && isAlreadyProceed) {
            return RESPONSE_RESET_RANGE_NOT_FROM_0;
        }

        return null;
    }

    public long reuseIdledSameInfoThresholdBytes() {
        return 10240;
    }

    public int determineBlockCount(@NonNull DownloadTask task, long totalLength) {
        if (task.connectionCount != null) return task.connectionCount;
        if (totalLength < ONE_CONNECTION_UPPER_LIMIT) return 1;
        if (totalLength < TWO_CONNECTION_UPPER_LIMIT) return 2;
        if (totalLength < THREE_CONNECTION_UPPER_LIMIT) return 3;
        if (totalLength < FOUR_CONNECTION_UPPER_LIMIT) {
            return 4;
        }

        return 5;
    }

    public boolean inspectAnotherSameInfo(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                          long instanceLength) {
        if (!task.filenameFromResponse) return false;
        final BreakpointStore store = OKDownload.with().breakpointStore;
        final BreakpointInfo anotherInfo = store.findAnotherInfoFromCompare(task, info);
        if (anotherInfo == null) return false;

        store.remove(anotherInfo.id);

        if (anotherInfo.getTotalOffset() <= OKDownload.with().downloadStrategy.reuseIdledSameInfoThresholdBytes()) return false;

        if (anotherInfo.etag != null && !anotherInfo.etag.equals(info.etag)) {
            return false;
        }

        if (anotherInfo.getTotalLength() != instanceLength) {
            return false;
        }

        if (anotherInfo.getFile() == null || !anotherInfo.getFile().exists()) return false;

        info.reuseBlocks(anotherInfo);

        LogUtil.d(TAG, "Reuse another same info: " + info);
        return true;
    }

    public boolean isServerCanceled(int responseCode, boolean isAlreadyProceed) {
        if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) return true;
        return responseCode == HttpURLConnection.HTTP_OK && isAlreadyProceed;
    }

    public void inspectNetworkAvailable() throws UnknownHostException {
        if (isHasAccessNetworkStatePermission == null) {
            isHasAccessNetworkStatePermission = ConnectionUtil
                    .checkPermission(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        // no permission will not check network available case.
        if (!isHasAccessNetworkStatePermission) return;

        if (manager == null) {
            manager = (ConnectivityManager) OKDownload.with().context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (!ConnectionUtil.isNetworkAvailable(manager)) {
            throw new UnknownHostException("network is not available!");
        }
    }

    public void inspectNetworkOnWifi(@NonNull DownloadTask task) throws IOException {
        if (isHasAccessNetworkStatePermission == null) {
            isHasAccessNetworkStatePermission = ConnectionUtil.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        if (!task.wifiRequired) return;

        if (!isHasAccessNetworkStatePermission) {
            throw new IOException("required for access network state but don't have the "
                    + "permission of Manifest.permission.ACCESS_NETWORK_STATE, please declare this "
                    + "permission first on your AndroidManifest, so we can handle the case of "
                    + "downloading required wifi state.");
        }

        if (manager == null) {
            manager = (ConnectivityManager) OKDownload.with().context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (ConnectionUtil.isNetworkNotOnWifiType(manager)) {
            throw new NetworkPolicyException();
        }
    }

    public void validFilenameFromResponse(@Nullable String responseFilename, @NonNull DownloadTask task, @NonNull BreakpointInfo info) throws IOException {
        if (TextUtils.isEmpty(task.getFilename())) {
            final String filename = determineFilename(responseFilename, task);

            if (TextUtils.isEmpty(task.getFilename())) {
                synchronized (task) {
                    if (TextUtils.isEmpty(task.getFilename())) {
                        task.filenameHolder.set(filename);
                        info.filenameHolder.set(filename);
                    }
                }
            }
        }
    }

    private static final Pattern TMP_FILE_NAME_PATTERN = Pattern
            .compile(".*\\\\|/([^\\\\|/|?]*)\\??");

    protected String determineFilename(@Nullable String responseFileName, @NonNull DownloadTask task) throws IOException {
        if (TextUtils.isEmpty(responseFileName)) {
            final String url = task.getUrl();
            Matcher m = TMP_FILE_NAME_PATTERN.matcher(url);
            String filename = null;
            while (m.find()) {
                filename = m.group(1);
            }

            if (TextUtils.isEmpty(filename)) {
                filename = Util.md5(url);
            }

            if (filename == null) {
                throw new IOException("Can't find valid filename.");
            }
            return filename;
        }
        return responseFileName;
    }

    public static class FilenameHolder {
        private volatile String filename;
        private final boolean filenameProvidedByConstruct;

        public FilenameHolder() {
            this.filenameProvidedByConstruct = false;
        }

        public FilenameHolder(@NonNull String filename) {
            this.filename = filename;
            this.filenameProvidedByConstruct = true;
        }

        void set(@NonNull String filename) {
            this.filename = filename;
        }

        @Nullable public String get() { return filename; }

        public boolean isFilenameProvidedByConstruct() {
            return filenameProvidedByConstruct;
        }

        @Override public boolean equals(Object obj) {
            if (super.equals(obj)) return true;

            if (obj instanceof FilenameHolder) {
                if (filename == null) {
                    return ((FilenameHolder) obj).filename == null;
                } else {
                    return filename.equals(((FilenameHolder) obj).filename);
                }
            }

            return false;
        }
        @Override public int hashCode() {
            return filename == null ? 0 : filename.hashCode();
        }
    }

    public ResumeAvailableResponseCheck resumeAvailableResponseCheck(
            DownloadConnection.Connected connected,
            int blockIndex,
            BreakpointInfo info) {
        return new ResumeAvailableResponseCheck(connected, blockIndex, info);
    }

    public static class ResumeAvailableResponseCheck {
        @NonNull private DownloadConnection.Connected connected;
        @NonNull private BreakpointInfo info;
        private int blockIndex;

        protected ResumeAvailableResponseCheck(@NonNull DownloadConnection.Connected connected,
                                               int blockIndex, @NonNull BreakpointInfo info) {
            this.connected = connected;
            this.info = info;
            this.blockIndex = blockIndex;
        }

        public void inspect() throws IOException {
            final BlockInfo blockInfo = info.getBlock(blockIndex);
            final int code = connected.getResponseCode();
            final String newEtag = connected.getResponseHeaderField(ETAG);

            final ResumeFailedCause resumeFailedCause = OKDownload.with().downloadStrategy
                    .getPreconditionFailedCause(code, blockInfo.getCurrentOffset() != 0,
                            info, newEtag);

            if (resumeFailedCause != null) {
                // resume failed, relaunch from beginning.
                throw new ResumeFailedException(resumeFailedCause);
            }

            final boolean isServerCancelled = OKDownload.with().downloadStrategy
                    .isServerCanceled(code, blockInfo.getCurrentOffset() != 0);

            if (isServerCancelled) {
                // server cancelled, end task.
                throw new ServerCanceledException(code, blockInfo.getCurrentOffset());
            }
        }
    }
}
