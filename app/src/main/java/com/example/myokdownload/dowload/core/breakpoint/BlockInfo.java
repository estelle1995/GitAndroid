package com.example.myokdownload.dowload.core.breakpoint;

import androidx.annotation.IntRange;

import com.example.myokdownload.dowload.core.Util;

import java.util.concurrent.atomic.AtomicLong;

public class BlockInfo {
    @IntRange(from = 0)
    private final long startOffset;
    @IntRange(from = 0)
    private final long contentLength;
    private final AtomicLong currentOffset;

    public BlockInfo(long startOffset, long contentLength) {
        this (startOffset, contentLength, 0);
    }

    public BlockInfo(long startOffset, long contentLength, @IntRange(from = 0) long currentOffset) {
        if (startOffset < 0 || (contentLength < 0 && contentLength != Util.CHUNKED_CONTENT_LENGTH) || currentOffset < 0) {
            throw new IllegalArgumentException();
        }
        this.startOffset = startOffset;
        this.contentLength = contentLength;
        this.currentOffset = new AtomicLong(currentOffset);
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getCurrentOffset() {
        return this.currentOffset.get();
    }
}
