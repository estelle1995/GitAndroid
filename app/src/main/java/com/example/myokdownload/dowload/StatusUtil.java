package com.example.myokdownload.dowload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointStore;
import com.example.myokdownload.dowload.core.dispatcher.DownloadDispatcher;

import java.io.File;

public class StatusUtil {

    public static boolean isSameTaskPendingOrRunning(@NonNull DownloadTask task) {
        return OKDownload.with().downloadDispatcher().findSameTask(task) != null;
    }

    public static Status getStatus(@NonNull DownloadTask task) {

        final Status status = isCompletedOrUnknown(task);
        if (status == Status.COMPLETED) return Status.COMPLETED;

        final DownloadDispatcher dispatcher = OKDownload.with().downloadDispatcher();

        if (dispatcher.isPending(task)) return Status.PENDING;
        if (dispatcher.isRunning(task)) return Status.RUNNING;

        return status;
    }

    public static Status getStatus(@NonNull String url, @NonNull String parentPath,
                                   @Nullable String filename) {
        return getStatus(createFinder(url, parentPath, filename));
    }

    public static boolean isCompleted(@NonNull DownloadTask task) {
        return isCompletedOrUnknown(task) == Status.COMPLETED;
    }

    public static Status isCompletedOrUnknown(@NonNull DownloadTask task) {
        final BreakpointStore store = OKDownload.with().breakpointStore();
        final BreakpointInfo info = store.get(task.getId());

        @Nullable String filename = task.getFilename();
        @NonNull final File parentFile = task.getParentFile();
        @Nullable final File targetFile = task.getFile();

        if (info != null) {
            if (!info.isChunked() && info.getTotalLength() <= 0) {
                return Status.UNKNOWN;
            } else if ((targetFile != null && targetFile.equals(info.getFile()))
                    && targetFile.exists()
                    && info.getTotalOffset() == info.getTotalLength()) {
                return Status.COMPLETED;
            } else if (filename == null && info.getFile() != null
                    && info.getFile().exists()) {
                return Status.IDLE;
            } else if (targetFile != null && targetFile.equals(info.getFile())
                    && targetFile.exists()) {
                return Status.IDLE;
            }
        } else if (store.isOnlyMemoryCache() || store.isFileDirty(task.getId())) {
            return Status.UNKNOWN;
        } else if (targetFile != null && targetFile.exists()) {
            return Status.COMPLETED;
        } else {
            filename = store.getResponseFilename(task.getUrl());
            if (filename != null && new File(parentFile, filename).exists()) {
                return Status.COMPLETED;
            }
        }

        return Status.UNKNOWN;
    }

    public static boolean isCompleted(@NonNull String url, @NonNull String parentPath,
                                      @Nullable String filename) {
        return isCompleted(createFinder(url, parentPath, filename));
    }

    @Nullable public static BreakpointInfo getCurrentInfo(@NonNull String url,
                                                          @NonNull String parentPath,
                                                          @Nullable String filename) {
        return getCurrentInfo(createFinder(url, parentPath, filename));
    }

    @Nullable public static BreakpointInfo getCurrentInfo(@NonNull DownloadTask task) {
        final BreakpointStore store = OKDownload.with().breakpointStore();
        final int id = store.findOrCreateId(task);

        final BreakpointInfo info = store.get(id);

        return info == null ? null : info.copy();
    }

    @NonNull static DownloadTask createFinder(@NonNull String url,
                                              @NonNull String parentPath,
                                              @Nullable String filename) {
        return new DownloadTask.Builder(url, parentPath, filename)
                .build();
    }

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        IDLE,
        // may completed, but no filename can't ensure.
        UNKNOWN
    }
}
