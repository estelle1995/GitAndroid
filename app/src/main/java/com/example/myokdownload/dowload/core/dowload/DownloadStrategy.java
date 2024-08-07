package com.example.myokdownload.dowload.core.dowload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DownloadStrategy {
    private static final String TAG = "DownloadStrategy";

    // 1 connection: [0, 1MB)
    private static final long ONE_CONNECTION_UPPER_LIMIT = 1024 * 1024; // 1MiB
    // 2 connection: [1MB, 5MB)
    private static final long TWO_CONNECTION_UPPER_LIMIT = 5 * 1024 * 1024; // 5MiB
    // 3 connection: [5MB, 50MB)
    private static final long THREE_CONNECTION_UPPER_LIMIT = 50 * 1024 * 1024; // 50MiB
    // 4 connection: [50MB, 100MB)
    private static final long FOUR_CONNECTION_UPPER_LIMIT = 100 * 1024 * 1024; // 100MiB

    public static class FilenameHolder {
        private volatile String filename;
        private final boolean filenameProvidedByConstruct;

        public FilenameHolder() {
            this.filenameProvidedByConstruct = false;
        }

        public FilenameHolder(@NonNull String filename) {
            this.filenameProvidedByConstruct = true;
            this.filename = filename;
        }

        void set(@NonNull String filename) {
            this.filename = filename;
        }

        @Nullable
        public String get() { return filename; }

        public boolean isFilenameProvidedByConstruct() {
            return filenameProvidedByConstruct;
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj)) return true;
            if (obj instanceof FilenameHolder) {
                if (filename == null) {
                    return ((FilenameHolder) obj).filename == null;
                } else {
                    return filename.equals(((FilenameHolder) obj).filename);
                }
            }
            return false;
        }

        @Override public int hashCode() {
            return filename == null ? 0 : filename.hashCode();
        }
    }
}
