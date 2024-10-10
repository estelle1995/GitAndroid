package com.example.myokdownload.dowload.core.download;

import static com.example.myokdownload.dowload.core.cause.ResumeFailedCause.OUTPUT_STREAM_NOT_SUPPORT;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.Util;
import com.example.myokdownload.dowload.core.breakpoint.BlockInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.cause.ResumeFailedCause;

import java.io.File;

public class BreakpointLocalCheck {

    private boolean dirty;
    boolean fileExist;
    boolean infoRight;
    boolean outputStreamSupport;

    private final DownloadTask task;
    private final BreakpointInfo info;
    private final long responseInstanceLength;

    public BreakpointLocalCheck(@NonNull DownloadTask task, @NonNull BreakpointInfo info,
                                long responseInstanceLength) {
        this.task = task;
        this.info = info;
        this.responseInstanceLength = responseInstanceLength;
    }

    @NonNull public ResumeFailedCause getCauseOrThrow() {
        if (!infoRight) {
            return ResumeFailedCause.INFO_DIRTY;
        } else if (!fileExist) {
            return ResumeFailedCause.FILE_NOT_EXIST;
        } else if (!outputStreamSupport) {
            return OUTPUT_STREAM_NOT_SUPPORT;
        }

        throw new IllegalStateException("No cause find with dirty: " + dirty);
    }

    public boolean isInfoRightToResume() {
        final int blockCount = info.getBlockCount();

        if (blockCount <= 0) return false;
        if (info.isChunked()) return false;
        if (info.getFile() == null) return false;
        final File fileOnTask = task.getFile();
        if (!info.getFile().equals(fileOnTask)) return false;
        if (info.getFile().length() > info.getTotalLength()) return false;

        if (responseInstanceLength > 0 && info.getTotalLength() != responseInstanceLength) {
            return false;
        }

        for (int i = 0; i < blockCount; i++) {
            BlockInfo blockInfo = info.getBlock(i);
            if (blockInfo.getContentLength() <= 0) return false;
        }

        return true;
    }

    public boolean isFileExistToResume() {
        final Uri uri = task.getUri();
        if (Util.isUriContentScheme(uri)) {
            return Util.getSizeFromContentUri(uri) > 0;
        } else {
            final File file = task.getFile();
            return file != null && file.exists();
        }
    }

    public boolean isDirty() {
        return dirty;
    }


    public void check() {
        fileExist = isFileExistToResume();
        infoRight = isInfoRightToResume();
        outputStreamSupport = isOutputStreamSupportResume();
        dirty = !infoRight || !fileExist || !outputStreamSupport;
    }

    public boolean isOutputStreamSupportResume() {
        final boolean supportSeek = OKDownload.with().outputStreamFactory().supportSeek();
        if (supportSeek) return true;

        if (info.getBlockCount() != 1) return false;
        return !OKDownload.with().processFileStrategy().isPreAllocateLength(task);
    }
}
