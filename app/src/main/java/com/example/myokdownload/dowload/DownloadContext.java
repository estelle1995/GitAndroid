package com.example.myokdownload.dowload;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadContext {
    private static final String TAG = "DownloadContext";
    private static final Executor SERIAL_EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new SynchronousQueue<>(),
            ThreadUtil.threadFactory("OKDownload Serial", false));

    private final DownloadTask[] tasks;
    volatile boolean started = false;
    @Nullable final DownloadContextListener contextListener;
    private final QueueSet set;
    private Handler uiHandler;

    DownloadContext(@NonNull DownloadTask[] tasks,
                    @Nullable DownloadContextListener contextListener,
                    @NonNull QueueSet set,
                    @NonNull Handler uiHandler) {
        this(tasks, contextListener, set);
        this.uiHandler = uiHandler;
    }

    DownloadContext(@NonNull DownloadTask[] tasks,
                    @Nullable DownloadContextListener contextListener,
                    @NonNull QueueSet set) {
        this.tasks = tasks;
        this.contextListener = contextListener;
        this.set = set;
    }

    public boolean isStarted() {
        return started;
    }

    public DownloadTask[] getTasks() {
        return tasks;
    }


    public static class QueueSet {
        private Map<String, List<String>> headerMapFields;
        private Uri uri;
        private Integer readBufferSize;
        private Integer flushBufferSize;
        private Integer syncBufferSize;
        private Integer syncBufferIntervalMillis;

        private Boolean autoCallbackToUIThread;
        private Integer minIntervalMillisCallbackProcess;

        private Boolean passIfAlreadyCompleted;
        private Boolean wifiRequired;

        private Object tag;

        public Map<String, List<String>> getHeaderMapFields() {
            return headerMapFields;
        }

        public void setHeaderMapFields(Map<String, List<String>> headerMapFields) {
            this.headerMapFields = headerMapFields;
        }

        public Uri getDirUri() {
            return uri;
        }

        public QueueSet setParentPathUri(@NonNull Uri uri) {
            this.uri = uri;
            return this;
        }

        public QueueSet setParentPathFile(@NonNull File parentPathFile) {
            if (parentPathFile.isFile()) {
                throw new IllegalArgumentException("parent path only accept directory path");
            }

            this.uri = Uri.fromFile(parentPathFile);
            return this;
        }

        public QueueSet setParentPath(@NonNull String parentPath) {
            return setParentPathFile(new File(parentPath));
        }

        public int getReadBufferSize() {
            return readBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_READ_BUFFER_SIZE : readBufferSize;
        }

        public QueueSet setReadBufferSize(int readBufferSize) {
            this.readBufferSize = readBufferSize;
            return this;
        }

        public QueueSet setWifiRequired(Boolean wifiRequired) {
            this.wifiRequired = wifiRequired;
            return this;
        }

        public boolean isWifiRequired() {
            return wifiRequired == null
                    ? DownloadTask.Builder.DEFAULT_IS_WIFI_REQUIRED : wifiRequired;
        }

        public int getFlushBufferSize() {
            return flushBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_FLUSH_BUFFER_SIZE : flushBufferSize;
        }

        public QueueSet setFlushBufferSize(int flushBufferSize) {
            this.flushBufferSize = flushBufferSize;
            return this;
        }

        public int getSyncBufferSize() {
            return syncBufferSize == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_SIZE : syncBufferSize;
        }

        public QueueSet setSyncBufferSize(int syncBufferSize) {
            this.syncBufferSize = syncBufferSize;
            return this;
        }

        public int getSyncBufferIntervalMillis() {
            return syncBufferIntervalMillis == null
                    ? DownloadTask.Builder.DEFAULT_SYNC_BUFFER_INTERVAL_MILLIS
                    : syncBufferIntervalMillis;
        }

        public QueueSet setSyncBufferIntervalMillis(int syncBufferIntervalMillis) {
            this.syncBufferIntervalMillis = syncBufferIntervalMillis;
            return this;
        }

        public boolean isAutoCallbackToUIThread() {
            return autoCallbackToUIThread == null
                    ? DownloadTask.Builder.DEFAULT_AUTO_CALLBACK_TO_UI_THREAD
                    : autoCallbackToUIThread;
        }

        public QueueSet setAutoCallbackToUIThread(Boolean autoCallbackToUIThread) {
            this.autoCallbackToUIThread = autoCallbackToUIThread;
            return this;
        }

        public int getMinIntervalMillisCallbackProcess() {
            return minIntervalMillisCallbackProcess == null
                    ? DownloadTask.Builder.DEFAULT_MIN_INTERVAL_MILLIS_CALLBACK_PROCESS
                    : minIntervalMillisCallbackProcess;
        }

        public QueueSet setMinIntervalMillisCallbackProcess(
                Integer minIntervalMillisCallbackProcess) {
            this.minIntervalMillisCallbackProcess = minIntervalMillisCallbackProcess;
            return this;
        }

        public Object getTag() {
            return tag;
        }

        public QueueSet setTag(Object tag) {
            this.tag = tag;
            return this;
        }

        public boolean isPassIfAlreadyCompleted() {
            return passIfAlreadyCompleted == null
                    ? DownloadTask.Builder.DEFAULT_PASS_IF_ALREADY_COMPLETED
                    : passIfAlreadyCompleted;
        }

        public QueueSet setPassIfAlreadyCompleted(boolean passIfAlreadyCompleted) {
            this.passIfAlreadyCompleted = passIfAlreadyCompleted;
            return this;
        }

        public Builder commit() {
            return new DownloadContext.Builder(this);
        }
    }

    public static class Builder {
        final ArrayList<DownloadTask> boundTaskList;

        private final QueueSet set;
        private DownloadContextListener listener;

        public Builder() {
            this(new QueueSet());
        }

        public Builder(QueueSet set) {
            this(set, new ArrayList<DownloadTask>());
        }

        public Builder(QueueSet set, ArrayList<DownloadTask> taskArrayList) {
            this.set = set;
            this.boundTaskList = taskArrayList;
        }

        public Builder setListener(DownloadContextListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder bindSetTask(@NonNull DownloadTask task) {
            final int index = boundTaskList.indexOf(task);
            if (index >= 0) {
                // replace
                boundTaskList.set(index, task);
            } else {
                boundTaskList.add(task);
            }

            return this;
        }

        public DownloadTask bind(@NonNull String url) {
            if (set.uri == null) {
                throw new IllegalArgumentException("If you want to bind only with url, you have to"
                        + " provide parentPath on QueueSet!");
            }

            return bind(new DownloadTask.Builder(url, set.uri).setFilenameFromResponse(true));
        }

        public DownloadTask bind(@NonNull DownloadTask.Builder taskBuilder) {
            if (set.headerMapFields != null) taskBuilder.setHeaderMapFields(set.headerMapFields);
            if (set.readBufferSize != null) taskBuilder.setReadBufferSize(set.readBufferSize);
            if (set.flushBufferSize != null) taskBuilder.setFlushBufferSize(set.flushBufferSize);
            if (set.syncBufferSize != null) taskBuilder.setSyncBufferSize(set.syncBufferSize);
            if (set.wifiRequired != null) taskBuilder.setWifiRequired(set.wifiRequired);
            if (set.syncBufferIntervalMillis != null) {
                taskBuilder.setSyncBufferIntervalMillis(set.syncBufferIntervalMillis);
            }
            if (set.autoCallbackToUIThread != null) {
                taskBuilder.setAutoCallbackToUIThread(set.autoCallbackToUIThread);
            }
            if (set.minIntervalMillisCallbackProcess != null) {
                taskBuilder
                        .setMinIntervalMillisCallbackProcess(set.minIntervalMillisCallbackProcess);
            }

            if (set.passIfAlreadyCompleted != null) {
                taskBuilder.setPassIfAlreadyCompleted(set.passIfAlreadyCompleted);
            }

            final DownloadTask task = taskBuilder.build();
            if (set.tag != null) task.setTag(set.tag);

            boundTaskList.add(task);
            return task;
        }

        public void unbind(@NonNull DownloadTask task) {
            boundTaskList.remove(task);
        }

        public void unbind(int id) {
            List<DownloadTask> list = (List<DownloadTask>) boundTaskList.clone();
            for (DownloadTask task : list) {
                if (task.getId() == id) boundTaskList.remove(task);
            }
        }

        public DownloadContext build() {
            DownloadTask[] tasks = new DownloadTask[boundTaskList.size()];
            return new DownloadContext(boundTaskList.toArray(tasks), listener, set);
        }
    }
}
