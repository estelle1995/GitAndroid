package com.example.myokdownload.dowload;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.IdentifiedTask;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.download.DownloadStrategy;
import com.example.myokdownload.dowload.core.log.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadTask extends IdentifiedTask {
    int id;
    String url;
    public Uri uri;
    public Map<String, List<String>> headerMapFields;

    private final int priority;

    public boolean autoCallbackToUIThread;
    public DownloadListener listener;
    public AtomicLong lastCallbackProcessTimestamp;
    public int minIntervalMillisCallbackProcess;
    BreakpointInfo info;
    public DownloadStrategy.FilenameHolder filenameHolder;
    File providedPathFile;
    File directoryFile;
    public Integer connectionCount;
    public int readBufferSize;
    private final boolean passIfAlreadyCompleted;

    public int flushBufferSize;
    public int syncBufferSize;
    public int syncBufferIntervalMills;
    @Nullable private File targetFile;
    public @Nullable Boolean isPreAllocateLength;
    public boolean wifiRequired;

    public String redirectLocation;
    public boolean filenameFromResponse;

    public DownloadTask(String url, Uri uri, int priority, int readBufferSize, int flushBufferSize,
                        int syncBufferSize, int syncBufferIntervalMills,
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
        this.lastCallbackProcessTimestamp = new AtomicLong();
        this.passIfAlreadyCompleted = passIfAlreadyCompleted;
        this.wifiRequired = wifiRequired;
        this.connectionCount = connectionCount;
        this.isPreAllocateLength = isPreAllocateLength;

        if (Util.isUriFileScheme(uri)) {
            final File file = new File(uri.getPath());
            if (filenameFromResponse != null) {
                if (filenameFromResponse) {
                    // filename must from response.
                    if (file.exists() && file.isFile()) {
                        // it have already provided file for it.
                        throw new IllegalArgumentException("If you want filename from "
                                + "response please make sure you provide path is directory "
                                + file.getPath());
                    }

                    if (!TextUtils.isEmpty(filename)) {
                        LogUtil.w("DownloadTask", "Discard filename[" + filename
                                + "] because you set filenameFromResponse=true");
                        filename = null;
                    }

                    directoryFile = file;
                } else {
                    // filename must not from response.
                    if (file.exists() && file.isDirectory() && TextUtils.isEmpty(filename)) {
                        // is directory but filename isn't provided.
                        // not valid filename found.
                        throw new IllegalArgumentException("If you don't want filename from"
                                + " response please make sure you have already provided valid "
                                + "filename or not directory path " + file.getPath());
                    }

                    if (TextUtils.isEmpty(filename)) {
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
                // not exist or is file.
                filenameFromResponse = false;

                if (file.exists()) {
                    // is file
                    if (!TextUtils.isEmpty(filename) && !file.getName().equals(filename)) {
                        throw new IllegalArgumentException("Uri already provided filename!");
                    }
                    filename = file.getName();
                    directoryFile = Util.getParentFile(file);
                } else {
                    // not exist
                    if (TextUtils.isEmpty(filename)) {
                        // filename is not provided, so we use the filename on path
                        filename = file.getName();
                        directoryFile = Util.getParentFile(file);
                    } else {
                        // filename is provided, so the path on file is directory
                        directoryFile = file;
                    }
                }
            }

            this.filenameFromResponse = filenameFromResponse;
        } else {
            this.filenameFromResponse = false;
            directoryFile = new File(uri.getPath());
        }

        if (TextUtils.isEmpty(filename)) {
            filenameHolder = new DownloadStrategy.FilenameHolder();
            providedPathFile = directoryFile;
        } else {
            filenameHolder = new DownloadStrategy.FilenameHolder(filename);
            targetFile = new File(directoryFile, filename);
            providedPathFile = targetFile;
        }

        this.id = OKDownload.with().breakpointStore.findOrCreateId(this);
    }

    public boolean isPassIfAlreadyCompleted() {
        return passIfAlreadyCompleted;
    }

    @Override
    public int getId() {
        return id;
    }

    @NonNull
    @Override
    public String getUrl() {
        return url;
    }

    @Nullable public File getFile() {
        final String filename = filenameHolder.get();
        if (filename == null) return null;
        if (targetFile == null) targetFile = new File(directoryFile, filename);

        return targetFile;
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

    @Override public boolean equals(Object obj) {
        if (super.equals(obj)) return true;

        if (obj instanceof DownloadTask) {
            final DownloadTask another = (DownloadTask) obj;
            if (another.id == this.id) return true;
            return compareIgnoreId(another);
        }

        return false;
    }

    @Override public int hashCode() {
        return (url + providedPathFile.toString() + filenameHolder.get()).hashCode();
    }


    public static class TaskHideWrapper {
        public static long getLastCallbackProcessTs(DownloadTask task) {
            return task.lastCallbackProcessTimestamp.get();
        }

        public static void setLastCallbackProcessTs(DownloadTask task,
                                                    long lastCallbackProcessTimestamp) {
            task.lastCallbackProcessTimestamp.set(lastCallbackProcessTimestamp);
        }

        public static void setBreakpointInfo(@NonNull DownloadTask task,
                                             @NonNull BreakpointInfo info) {
            task.info = info;
        }
    }

    /**
     * The builder of download task.
     */
    public static class Builder {
        @NonNull final String url;
        @NonNull final Uri uri;
        private volatile Map<String, List<String>> headerMapFields;

        /**
         * Create the task builder through {@code url} and the file's parent path and the filename.
         *
         * @param url        the url for the task.
         * @param parentPath the parent path of the file for store download data.
         * @param filename   the filename of the file for store download data.
         */
        public Builder(@NonNull String url, @NonNull String parentPath, @Nullable String filename) {
            this(url, Uri.fromFile(new File(parentPath)));
            if (TextUtils.isEmpty(filename)) {
                this.isFilenameFromResponse = true;
            } else {
                this.filename = filename;
            }
        }

        /**
         * Create the task builder through {@code url} and the store path {@code file}.
         *
         * @param url  the url for the task.
         * @param file the file is used for store download data of the task.
         */
        public Builder(@NonNull String url, @NonNull File file) {
            this.url = url;
            this.uri = Uri.fromFile(file);
        }

        /**
         * Create the task builder through {@code url} and the store path {@code uri}.
         *
         * @param url the url for the task.
         * @param uri the uri indicate the file path of the task.
         */
        public Builder(@NonNull String url, @NonNull Uri uri) {
            this.url = url;
            this.uri = uri;
            if (Util.isUriContentScheme(uri)) {
                this.filename = Util.getFilenameFromContentUri(uri);
            }
        }

        // More larger more high.
        private int priority;

        public static final int DEFAULT_READ_BUFFER_SIZE = 4096/* byte **/;
        private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
        public static final int DEFAULT_FLUSH_BUFFER_SIZE = 16384/* byte **/;
        private int flushBufferSize = DEFAULT_FLUSH_BUFFER_SIZE;

        /**
         * Make sure sync to physical filesystem.
         */
        public static final int DEFAULT_SYNC_BUFFER_SIZE = 65536/* byte **/;
        private int syncBufferSize = DEFAULT_SYNC_BUFFER_SIZE;
        public static final int DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS = 2000/* millis **/;
        private int syncBufferIntervalMillis = DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS;

        public static final boolean DEFAULT_AUTO_CALLBACK_TO_UI_THREAD = true;
        private boolean autoCallbackToUIThread = DEFAULT_AUTO_CALLBACK_TO_UI_THREAD;

        public static final int DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS = 3000/* millis **/;
        private int minIntervalMillisCallbackProcess = DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS;

        private String filename;

        public static final boolean DEFAULT_PASS_IF_ALREADY_COMPLETED = true;
        /**
         * if this task has already completed judged by
         * {@link StatusUtil.Status#isCompleted(DownloadTask)}, callback completed directly instead
         * of start download.
         */
        private boolean passIfAlreadyCompleted = DEFAULT_PASS_IF_ALREADY_COMPLETED;

        public static final boolean DEFAULT_IS_WIFI_REQUIRED = false;

        private boolean isWifiRequired = DEFAULT_IS_WIFI_REQUIRED;

        private Boolean isFilenameFromResponse;
        private Integer connectionCount;
        private Boolean isPreAllocateLength;

        /**
         * Set whether need to pre allocate length for the file after get the resource-length from
         * trial-connection.
         *
         * @param preAllocateLength whether need to pre allocate length for the file before
         *                          download.
         */
        public Builder setPreAllocateLength(boolean preAllocateLength) {
            isPreAllocateLength = preAllocateLength;
            return this;
        }

        /**
         * Set the count of connection establish for this task, if this task has already split block
         * on the past and waiting for resuming, this set connection count will not effect really.
         *
         * @param connectionCount the count of connection establish for this task.
         */
        public Builder setConnectionCount(@IntRange(from = 1) int connectionCount) {
            this.connectionCount = connectionCount;
            return this;
        }

        /**
         * Set whether the provided Uri or path is just directory, and filename must be from
         * response header or url path.
         * <p>
         * If you provided {@link #filename} the filename will be invalid for this supposed.
         * If you provided content scheme Uri, this value is unaccepted.
         *
         * @param filenameFromResponse whether the provided Uri or path is just directory, and
         *                             filename must be from response header or url path.
         *                             if {@code null} this value will be discard.
         */
        public Builder setFilenameFromResponse(@Nullable Boolean filenameFromResponse) {
            if (!Util.isUriFileScheme(uri)) {
                throw new IllegalArgumentException(
                        "Uri isn't file scheme we can't let filename from response");
            }

            isFilenameFromResponse = filenameFromResponse;

            return this;
        }

        /**
         * Set whether callback to UI thread automatically.
         * default is {@link #DEFAULT_AUTO_CALLBACK_TO_UI_THREAD}
         *
         * @param autoCallbackToUIThread whether callback to ui thread automatically.
         */
        public Builder setAutoCallbackToUIThread(boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            return this;
        }

        /**
         * Set the minimum internal milliseconds of progress callbacks.
         * default is {@link #DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS}
         *
         * @param minIntervalMillisCallbackProcess the minimum interval milliseconds of  progress
         *                                         callbacks.
         */
        public Builder setMinIntervalMillisCallbackProcess(int minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        /**
         * Set the request headers for this task.
         *
         * @param headerMapFields the header map fields.
         */
        public Builder setHeaderMapFields(Map<String, List<String>> headerMapFields) {
            this.headerMapFields = headerMapFields;
            return this;
        }

        /**
         * Add the request header for this task.
         *
         * @param key   the key of the field.
         * @param value the value of the field.
         */
        public synchronized void addHeader(String key, String value) {
            if (headerMapFields == null) headerMapFields = new HashMap<>();
            List<String> valueList = headerMapFields.get(key);
            if (valueList == null) {
                valueList = new ArrayList<>();
                headerMapFields.put(key, valueList);
            }
            valueList.add(value);
        }

        /**
         * Set the priority of the task, more larger more higher, more higher means less time to
         * wait to download.
         * default is 0.
         *
         * @param priority the priority of the task.
         */
        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Set the how may bytes of buffer once read from response input-stream.
         * default is {@link #DEFAULT_READ_BUFFER_SIZE}
         *
         * @param readBufferSize the bytes of buffer once read from response input-stream.
         */
        public Builder setReadBufferSize(int readBufferSize) {
            if (readBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.readBufferSize = readBufferSize;
            return this;
        }

        /**
         * Set the hwo many bytes of the buffer size on the BufferedOutputStream.
         * default is {@link #DEFAULT_FLUSH_BUFFER_SIZE}
         *
         * @param flushBufferSize the bytes of buffer size before BufferedOutputStream#flush().
         */
        public Builder setFlushBufferSize(int flushBufferSize) {
            if (flushBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.flushBufferSize = flushBufferSize;
            return this;
        }

        /**
         * Set the how many bytes of the buffer size before sync to the disk.
         * default is {@link #DEFAULT_SYNC_BUFFER_SIZE}
         *
         * @param syncBufferSize the bytes of buffer size before sync.
         */
        public Builder setSyncBufferSize(int syncBufferSize) {
            if (syncBufferSize < 0) throw new IllegalArgumentException("Value must be positive!");

            this.syncBufferSize = syncBufferSize;
            return this;
        }

        /**
         * Set the interval milliseconds for sync download-data buffer from the memory to the disk.
         * default is {@link #DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS}
         *
         * @param syncBufferIntervalMillis the interval milliseconds for sync buffer to the disk.
         */
        public Builder setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            if (syncBufferIntervalMillis < 0) {
                throw new IllegalArgumentException("Value must be positive!");
            }

            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
            return this;
        }

        /**
         * Set the filename of the file for this task.
         * <p>
         * If you only provided the store directory path, and doesn't provide any filename, the
         * filename will get through response header, and if there isn't filename found on the
         * response header, the file name will be found through the url path.
         *
         * @param filename the filename of the file for this task.
         */
        public Builder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * Set whether the task is completed directly without any further action when check the task
         * has been downloaded.
         * default is {@link #DEFAULT_PASS_IF_ALREADY_COMPLETED}
         *
         * @param passIfAlreadyCompleted whether pass this task with completed callback directly if
         *                               this task has already completed.
         */
        public Builder setPassIfAlreadyCompleted(boolean passIfAlreadyCompleted) {
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            return this;
        }

        /**
         * Set the task proceed only on the Wifi network state.
         * default is {@link #DEFAULT_IS_WIFI_REQUIRED}
         *
         * @param wifiRequired whether wifi required for proceed this task.
         */
        public Builder setWifiRequired(boolean wifiRequired) {
            this.isWifiRequired = wifiRequired;
            return this;
        }

        /**
         * Build the task through the builder.
         *
         * @return a new task is built from this builder.
         */
        public DownloadTask build() {
            return new DownloadTask(url, uri, priority, readBufferSize, flushBufferSize,
                    syncBufferSize, syncBufferIntervalMillis,
                    autoCallbackToUIThread, minIntervalMillisCallbackProcess,
                    headerMapFields, filename, passIfAlreadyCompleted, isWifiRequired,
                    isFilenameFromResponse, connectionCount, isPreAllocateLength);
        }
    }
}
