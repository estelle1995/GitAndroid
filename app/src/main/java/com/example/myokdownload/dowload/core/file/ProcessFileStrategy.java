package com.example.myokdownload.dowload.core.file;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;

import java.io.File;
import java.io.IOException;

public class ProcessFileStrategy {
    private final FileLock fileLock = new FileLock();

    @NonNull public MultiPointOutputStream createProcessStream(@NonNull DownloadTask task,
                                                               @NonNull BreakpointInfo info,
                                                               @NonNull DownloadStore store) {
        return new MultiPointOutputStream(task, info, store);
    }

    public void completeProcessStream(@NonNull MultiPointOutputStream processOutputStream, @NonNull DownloadTask task) {

    }

    public void discardProcess(@NonNull DownloadTask task) throws IOException {
        final File file = task.getFile();
        if (file == null) return;
        if (file.exists() && !file.delete()) {
            throw new IOException("Delete file failed!");
        }
    }

    @NonNull public FileLock getFileLock() {
        return fileLock;
    }

    public boolean isPreAllocateLength(@NonNull DownloadTask task) {
        boolean supportSeek = OKDownload.with().outputStreamFactory().supportSeek();
        if (!supportSeek) return false;
        if (task.isPreAllocateLength != null) return task.isPreAllocateLength;
        return true;
    }
}
