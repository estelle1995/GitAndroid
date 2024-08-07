package com.example.myokdownload.dowload.core.breakpoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;

public interface BreakpointStore {
    @Nullable
    BreakpointInfo get(int id);

    int findOrCreateId(@NonNull DownloadTask task);

    boolean isOnlyMemoryCache();

    boolean isFileDirty(int id);

    @Nullable
    String getResponseFilename(String url);
}
