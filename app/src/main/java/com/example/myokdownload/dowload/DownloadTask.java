package com.example.myokdownload.dowload;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.core.IdentifiedTask;

import java.io.File;

public class DownloadTask extends IdentifiedTask {
    @Override
    public int getId() {
        return 0;
    }

    @NonNull
    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public Uri getUri() {
        return null;
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
}
