package com.example.myokdownload.dowload.core.download;

import static com.example.myokdownload.dowload.core.connection.ConnectionUtil.RANGE_NOT_SATISFIABLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.cause.ResumeFailedCause;
import com.example.myokdownload.dowload.core.exception.FileBusyAfterRunException;
import com.example.myokdownload.dowload.core.exception.ServerCanceledException;

import java.io.IOException;

public class BreakpointRemoteCheck {
    private boolean acceptRange;
    private boolean resumable;
    ResumeFailedCause failedCause;
    private long instanceLength;

    @NonNull
    private final DownloadTask task;
    @NonNull private final BreakpointInfo info;

    public BreakpointRemoteCheck(@NonNull DownloadTask task,
                                 @NonNull BreakpointInfo info) {
        this.task = task;
        this.info = info;
    }

    @Override
    public String toString() {
        return "acceptRange[" + acceptRange + "] "
                + "resumable[" + resumable + "] "
                + "failedCause[" + failedCause + "] "
                + "instanceLength[" + instanceLength + "] "
                + super.toString();
    }

    @Nullable
    public ResumeFailedCause getCause() {
        return this.failedCause;
    }

    @NonNull public ResumeFailedCause getCauseOrThrow() {
        if (failedCause == null) {
            throw new IllegalStateException("No cause find with resumable: " + resumable);
        }
        return this.failedCause;
    }

    public boolean isResumable() {
        return resumable;
    }

    public boolean isAcceptRange() {
        return acceptRange;
    }

    public long getInstanceLength() {
        return instanceLength;
    }

    public void check() throws IOException {
        final DownloadStrategy downloadStrategy = OKDownload.with().downloadStrategy;

        ConnectTrial connectTrial = createConnectTrial();
        connectTrial.executeTrial();

        final boolean isAcceptRange = connectTrial.isAcceptRange();;
        final boolean isChunked = connectTrial.isChunked();

        final long instanceLength = connectTrial.getInstanceLength();
        final String responseEtag = connectTrial.getResponseEtag();
        final String responseFilename = connectTrial.getResponseFilename();
        final int responseCode = connectTrial.getResponseCode();

        downloadStrategy.validFilenameFromResponse(responseFilename, task, info);
        info.chunked = isChunked;
        info.etag = responseEtag;

        if (OKDownload.with().downloadDispatcher.isFileConflictAfterRun(task)) {
            throw FileBusyAfterRunException.SIGNAL;
        }

        // 2. collect result
        final ResumeFailedCause resumeFailedCause = downloadStrategy.getPreConditionFailCause(responseCode, info.getTotalOffset() != 0, info, responseEtag);

        this.resumable = resumeFailedCause == null;
        this.failedCause = resumeFailedCause;
        this.instanceLength = instanceLength;
        this.acceptRange = isAcceptRange;

        //3. check whether server cancelled.
        //3. check whether server cancelled.
        if (!isTrialSpecialPass(responseCode, instanceLength, resumable)
                && downloadStrategy.isServerCanceled(responseCode, info.getTotalOffset() != 0)) {
            throw new ServerCanceledException(responseCode, info.getTotalOffset());
        }
    }

    boolean isTrialSpecialPass(int responseCode, long instanceLength, boolean isResumable) {
        if (responseCode == RANGE_NOT_SATISFIABLE && instanceLength >= 0 && isResumable) {
            return true;
        }
        return false;
    }

    ConnectTrial createConnectTrial() {
        return new ConnectTrial(task, info);
    }
}
