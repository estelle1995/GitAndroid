package com.example.myokdownload.dowload;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.IdentifiedTask;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.download.DownloadStrategy;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadTask extends IdentifiedTask {
    public Uri uri;
    public Map<String, List<String>> headerMapFields;

    public boolean autoCallbackToUIThread;
    public DownloadListener listener;
    public AtomicLong lastCallbackProcessTimestamp;
    public int minIntervalMillisCallbackProcess;
    BreakpointInfo info;
    public DownloadStrategy.FilenameHolder filenameHolder;
    File directoryFile;
    public Integer connectionCount;

    public int flushBufferSize;
    public int syncBufferSize;
    public int syncBufferIntervalMills;
    @Nullable private File targetFile;
    public @Nullable Boolean isPreAllocateLength;
    public boolean wifiRequired;

    public String redirectLocation;
    public boolean filenameFromResponse;

    @Override
    public int getId() {
        return 0;
    }

    @NonNull
    @Override
    public String getUrl() {
        return "";
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
        return null;
    }

    @NonNull
    @Override
    public File getParentFile() {
        return null;
    }

    @Nullable
    @Override
    public String getFilename() {
        return "";
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
}
