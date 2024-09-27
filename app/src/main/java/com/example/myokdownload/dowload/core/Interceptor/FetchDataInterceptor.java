package com.example.myokdownload.dowload.core.Interceptor;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.dispatcher.CallbackDispatcher;
import com.example.myokdownload.dowload.core.download.DownloadChain;
import com.example.myokdownload.dowload.core.exception.InterruptException;
import com.example.myokdownload.dowload.core.file.MultiPointOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class FetchDataInterceptor implements Interceptor.Fetch {
    private final InputStream inputStream;

    private final byte[] readBuffer;
    private final MultiPointOutputStream outputStream;
    private final int blockIndex;
    private final DownloadTask task;
    private final CallbackDispatcher dispatcher;

    public FetchDataInterceptor(int blockIndex, @NonNull InputStream inputStream, @NonNull MultiPointOutputStream outputStream, DownloadTask task) {
        this.blockIndex = blockIndex;
        this.inputStream = inputStream;
        this.readBuffer = new byte[task.readBufferSize];
        this.outputStream = outputStream;

        this.task = task;
        this.dispatcher = OKDownload.with().callbackDispatcher;
    }

    @Override
    public long interceptFetch(DownloadChain chain) throws IOException {
        if (chain.getCache().isInterrupt()) {
            throw InterruptException.SIGNAL;
        }
        OKDownload.with().downloadStrategy.inspectNetworkOnWifi(chain.getTask());
        //fetch
        int fetchLength = inputStream.read(readBuffer);
        if (fetchLength == -1) {
            return fetchLength;
        }

        //write to file
        outputStream.write(blockIndex, readBuffer, fetchLength);

        chain.increaseCallbackBytes(fetchLength);
        if (this.dispatcher.isFetchProcessMoment(task)) {
            chain.flushNoCallbackIncreaseBytes();
        }
        return fetchLength;
    }
}
