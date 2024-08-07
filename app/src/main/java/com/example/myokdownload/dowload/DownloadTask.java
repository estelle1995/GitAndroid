package com.example.myokdownload.dowload;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.IdentifiedTask;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.dowload.DownloadStrategy;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadTask extends IdentifiedTask implements Comparable<DownloadTask> {
    private final int id;

    @NonNull private final String url;
    private final Uri uri;
    private final Map<String, List<String>> headerMapFields;
    private final int priority;

    // optimize ------------------
    // no progress callback
    // no request callback
    // no response callback

    private final int readBufferSize;
    private final int flushBufferSize;

    private final int syncBufferSize;
    private final int syncBufferIntervalMills;
    @Nullable private final Integer connectionCount;
    @Nullable private final Boolean isPreAllocateLength;
    private final boolean autoCallbackToUIThread;
    private final int minIntervalMillisCallbackProcess;
    private final boolean filenameFromResponse;
    private final boolean passIfAlreadyCompleted;
    private final boolean wifiRequired;

    private final AtomicLong lastCallbackProcessTimestamp;

    @NonNull private final File directoryFile;

    @NonNull private final DownloadStrategy.FilenameHolder filenameHolder;
    @NonNull private final File providedPathFile;

    @Nullable private File targetFile;

    public DownloadTask(String url, Uri uri, int priority, int readBufferSize,
                        int flushBufferSize, int syncBufferSize, int syncBufferIntervalMills,
                        boolean autoCallbackToUIThread, int minIntervalMillisCallbackProcess,
                        Map<String, List<String>> headerMapFields, @Nullable String filename,
                        boolean passIfAlreadyCompleted, boolean wifiRequired,
                        Boolean filenameFromResponse, @Nullable Integer connectionCount,
                        @Nullable Boolean isPreAllocateLength) {
        this.url = url;
        this.uri = uri;
        this.priority = priority;
        this.readBufferSize = readBufferSize;
        this.flushBufferSize = flushBufferSize;
        this.syncBufferSize = syncBufferSize;
        this.syncBufferIntervalMills = syncBufferIntervalMills;
        this.autoCallbackToUIThread = autoCallbackToUIThread;
        this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
        this.headerMapFields = headerMapFields;
        this.passIfAlreadyCompleted = passIfAlreadyCompleted;
        this.wifiRequired = wifiRequired;
        this.connectionCount = connectionCount;
        this.isPreAllocateLength = isPreAllocateLength;
        this.lastCallbackProcessTimestamp = new AtomicLong();

        if (Util.isUriFileScheme(uri)) {
            final File file = new File(uri.getPath());
            if (filenameFromResponse != null) {
                if (filenameFromResponse) {
                    if (file.exists() && file.isFile()) {
                        // it have already provided file for it.
                        throw new IllegalArgumentException("If you want filename from "
                                + "response please make sure you provide path is directory "
                                + file.getPath());
                    }

                    if (!Util.isEmpty(filename)) {
                        Util.w("DownloadTask", "Discard filename[" + filename
                                + "] because you set filenameFromResponse=true");
                        filename = null;
                    }

                    directoryFile = file;
                } else {
                    if (file.exists() && file.isDirectory() && Util.isEmpty(filename)) {
                        throw new IllegalArgumentException("If you don't want filename from"
                                + " response please make sure you have already provided valid "
                                + "filename or not directory path " + file.getPath());
                    }

                    if (Util.isEmpty(filename)) {
                        filename = file.getName();
                        directoryFile = Util.getParentFile(file);
                    } else {
                        directoryFile = file;
                    }
                }
            } else if (file.exists() && file.isDirectory()) {
                filenameFromResponse = true;
                directoryFile = file;
            } else {
                filenameFromResponse = false;

                if (file.exists()) {
                    if (!Util.isEmpty(filename) && !file.getName().equals(filename)) {
                        throw new IllegalArgumentException("Uri already provided filename!");
                    }
                    filename = file.getName();
                    directoryFile = Util.getParentFile(file);
                } else {
                    if (Util.isEmpty(filename)) {
                        filename = file.getName();
                        directoryFile = Util.getParentFile(file);
                    } else {
                        directoryFile = file;
                    }
                }
            }

            this.filenameFromResponse = filenameFromResponse;
        } else {
            this.filenameFromResponse = false;
            directoryFile = new File(uri.getPath());
        }

        if (Util.isEmpty(filename)) {
            filenameHolder = new DownloadStrategy.FilenameHolder();
            providedPathFile = directoryFile;
        } else {
            filenameHolder = new DownloadStrategy.FilenameHolder(filename);
            targetFile = new File(directoryFile, filename);
            providedPathFile = targetFile;
        }

        this.id = OkDownload.with().breakpointStore().findOrCreateId(this);
    }

    @Override
    public int getId() {
        return id;
    }

    public File getFile() {
        final String filename = filenameHolder.get();
        if (filename == null) return null;
        if (targetFile == null) targetFile = new File(directoryFile, filename);
        return targetFile;
    }

    public Uri getUri() {
        return uri;
    }

    @NonNull
    @Override
    public String getUrl() {
        return url;
    }

    @NonNull
    @Override
    protected File getProvidedPathFile() {
        return providedPathFile;
    }

    @NonNull
    @Override
    public File getParentFile() {
        return directoryFile;
    }

    @Nullable
    @Override
    public String getFilename() {
        return filenameHolder.get();
    }

    public boolean isFilenameFromResponse() {
        return filenameFromResponse;
    }

    @NonNull public MockTaskForCompare mock(int id) {
        return new MockTaskForCompare(id, this);
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(DownloadTask downloadTask) {
        return downloadTask.getPriority() - priority;
    }

    public static class Builder {
        @NonNull final String url;
        @NonNull final Uri uri;
        private volatile Map<String, List<String>> headerMapFields;

        private String filename;
        private Boolean isFilenameFromResponse;

        public Builder(@NonNull String url, @NonNull String parentPath, @Nullable String filename) {
            this(url, Uri.fromFile(new File(parentPath)));
            if (Util.isEmpty(filename)) {
                this.isFilenameFromResponse = true;
            } else {
                this.filename = filename;
            }
        }

        public Builder(@NonNull String url, @NonNull File file) {
            this.url = url;
            this.uri = Uri.fromFile(file);
        }

        public Builder(@NonNull String url, @NonNull Uri uri) {
            this.url = url;
            this.uri = uri;
            if (Util.isUriContentScheme(uri)) {
                this.filename = Util.getFilenameFromContentUri(uri);
            }
        }

        private int priority;
        public static final int DEFAULT_READ_BUFFER_SIZE = 4096/* byte **/;
        private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
        public static final int DEFAULT_FLUSH_BUFFER_SIZE = 16384/* byte **/;
        private int flushBufferSize = DEFAULT_FLUSH_BUFFER_SIZE;

        public static final int DEFAULT_SYNC_BUFFER_SIZE = 65536/* byte **/;
        private int syncBufferSize = DEFAULT_SYNC_BUFFER_SIZE;
        public static final int DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS = 2000/* millis **/;
        private int syncBufferIntervalMillis = DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS;

        public static final boolean DEFAULT_AUTO_CALLBACK_TO_UI_THREAD = true;
        private boolean autoCallbackToUIThread = DEFAULT_AUTO_CALLBACK_TO_UI_THREAD;

        public static final int DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS = 3000/* millis **/;
        private int minIntervalMillisCallbackProcess = DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS;
        public static final boolean DEFAULT_PASS_IF_ALREADY_COMPLETED = true;
        private boolean passIfAlreadyCompleted = DEFAULT_PASS_IF_ALREADY_COMPLETED;

        public static final boolean DEFAULT_IS_WIFI_REQUIRED = false;

        private boolean isWifiRequired = DEFAULT_IS_WIFI_REQUIRED;
        private Integer connectionCount;
        private Boolean isPreAllocateLength;

        public Builder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder setMinIntervalMillisCallbackProcess(int minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        public Builder setPassIfAlreadyCompleted(boolean passIfAlreadyCompleted) {
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            return this;
        }

        public DownloadTask build() {
            return new DownloadTask(url, uri, priority, readBufferSize, flushBufferSize,
                    syncBufferSize, syncBufferIntervalMillis,
                    autoCallbackToUIThread, minIntervalMillisCallbackProcess,
                    headerMapFields, filename, passIfAlreadyCompleted, isWifiRequired,
                    isFilenameFromResponse, connectionCount, isPreAllocateLength);
        }
    }

    public static class MockTaskForCompare extends IdentifiedTask {
        final int id;
        @NonNull final String url;
        @NonNull final File providedPathFile;
        @Nullable final String filename;
        @NonNull final File parentFile;

        public MockTaskForCompare(int id) {
            this.id = id;
            this.url = EMPTY_URL;
            this.providedPathFile = EMPTY_FILE;
            this.filename = null;
            this.parentFile = EMPTY_FILE;
        }

        public MockTaskForCompare(int id, @NonNull DownloadTask task) {
            this.id = id;
            this.url = task.url;
            this.parentFile = task.getParentFile();
            this.providedPathFile = task.providedPathFile;
            this.filename = task.getFilename();
        }

        @Override public int getId() {
            return id;
        }

        @NonNull @Override public String getUrl() {
            return url;
        }

        @NonNull @Override protected File getProvidedPathFile() {
            return providedPathFile;
        }

        @NonNull @Override public File getParentFile() {
            return parentFile;
        }

        @Nullable @Override public String getFilename() {
            return filename;
        }
    }
}
