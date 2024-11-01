package com.example.myokdownload.dowload.core.dowload;

public class DownloadCache {
    private volatile boolean userCanceled;

    void setUserCanceled() {
        this.userCanceled = true;
    }
}
