package com.example.myokdownload.dowload.core.breakpoint;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.download.DownloadStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BreakpointInfo {
    public int id;

    public String etag;
    public String url;

    public DownloadStrategy.FilenameHolder filenameHolder;
    public boolean chunked;

    File parentFile;
    private File targetFile;

    private List<BlockInfo> blockInfoList;
    private final boolean taskOnlyProvidedParentPath;

    public BreakpointInfo(int id, @NonNull String url, @NonNull File parentFile,
                          @Nullable String filename) {
        this.id = id;
        this.url = url;
        this.parentFile = parentFile;
        this.blockInfoList = new ArrayList<>();

        if (TextUtils.isEmpty(filename)) {
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

        if (TextUtils.isEmpty(filename)) {
            filenameHolder = new DownloadStrategy.FilenameHolder();
        } else {
            filenameHolder = new DownloadStrategy.FilenameHolder(filename);
        }

        this.taskOnlyProvidedParentPath = taskOnlyProvidedParentPath;
    }


    public BlockInfo getBlock(int blockIndex) {
        return blockInfoList.get(blockIndex);
    }

    public long getTotalOffset() {
        long offset = 0;
        final Object[] blocks = blockInfoList.toArray();
        for (Object block : blocks) {
            if (block instanceof BlockInfo) {
                offset += ((BlockInfo) block).getCurrentOffset();
            }
        }
        return offset;
    }

    public long getTotalLength() {
        if (!chunked) return getTotalOffset();

        long length = 0;
        final Object[] blocks = blockInfoList.toArray();
        for (Object block: blocks) {
            if (block instanceof BlockInfo) {
                length += ((BlockInfo) block).getContentLength();
            }
        }

        return length;
    }

    @Nullable
    public File getFile() {
        final String filename = this.filenameHolder.get();
        if (filename == null) return null;
        if (targetFile == null) targetFile = new File(parentFile, filename);

        return targetFile;
    }

    public void addBlock(BlockInfo blockInfo) {
        this.blockInfoList.add(blockInfo);
    }

    public void reuseBlocks(BreakpointInfo info) {
        blockInfoList.clear();
        blockInfoList.addAll(info.blockInfoList);
    }

    public int getBlockCount() {
        return blockInfoList.size();
    }

    public void resetBlockInfos() {
        this.blockInfoList.clear();
    }

    public BreakpointInfo copy() {
        final BreakpointInfo info = new BreakpointInfo(id, url, parentFile, filenameHolder.get(),
                taskOnlyProvidedParentPath);
        info.chunked = this.chunked;
        for (BlockInfo blockInfo : blockInfoList) {
            info.blockInfoList.add(blockInfo.copy());
        }
        return info;
    }
}
