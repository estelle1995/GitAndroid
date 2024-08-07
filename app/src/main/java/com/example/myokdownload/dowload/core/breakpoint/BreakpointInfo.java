package com.example.myokdownload.dowload.core.breakpoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.dowload.DownloadStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BreakpointInfo {
    final int id;
    private final String url;
    @NonNull final File parentFile;

    private final DownloadStrategy.FilenameHolder filenameHolder;
    @Nullable private File targetFile;

    private final boolean taskOnlyProvidedParentPath;

    private boolean chunked;

    private final List<BlockInfo> blockInfoList;

    public BreakpointInfo(int id, @NonNull String url, @NonNull File parentFile,
                          @Nullable String filename) {
        this.id = id;
        this.url = url;
        this.parentFile = parentFile;
        this.blockInfoList = new ArrayList<>();

        if (Util.isEmpty(filename)) {
            filenameHolder = new DownloadStrategy.FilenameHolder();
            taskOnlyProvidedParentPath = true;
        } else {
            filenameHolder = new DownloadStrategy.FilenameHolder(filename);
            taskOnlyProvidedParentPath = false;
            targetFile = new File(parentFile, filename);
        }
    }

    BreakpointInfo(int id, @NonNull String url, @NonNull File parentFile,
                   @Nullable String filename, boolean taskOnlyProvidedParentPath) {
        this.id = id;
        this.url = url;
        this.parentFile = parentFile;
        this.blockInfoList = new ArrayList<>();

        if (Util.isEmpty(filename)) {
            filenameHolder = new DownloadStrategy.FilenameHolder();
        } else {
            filenameHolder = new DownloadStrategy.FilenameHolder(filename);
        }

        this.taskOnlyProvidedParentPath = taskOnlyProvidedParentPath;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public boolean isChunked() {
        return chunked;
    }

    public long getTotalOffset() {
        long offset = 0;
        for (BlockInfo block: blockInfoList) {
            offset += block.getCurrentOffset();
        }
        return offset;
    }

    @Nullable public File getFile() {
        final String filename = this.filenameHolder.get();
        if (filename == null) return null;
        if (targetFile == null) targetFile = new File(parentFile, filename);
        return targetFile;
    }

    public long getTotalLength() {
        if (isChunked()) return getTotalOffset();

        long length = 0;
        for (BlockInfo block: blockInfoList) {
            length += block.getContentLength();
        }
        return length;
    }

    public boolean isSameFrom(DownloadTask task) {
        if (!parentFile.equals(task.getParentFile())) {
            return false;
        }
        if (!url.equals(task.getUrl())) return false;

        final String otherFilename = task.getFilename();
        if (otherFilename != null && otherFilename.equals(filenameHolder.get())) return true;

        if (taskOnlyProvidedParentPath) {
            if (!task.isFilenameFromResponse()) return false;
            return otherFilename == null || otherFilename.equals(filenameHolder.get());
        }
        return false;
    }
}
