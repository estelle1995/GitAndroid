package com.example.myokdownload.dowload.core;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.StatFs;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.breakpoint.BlockInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointStoreOnCache;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.connection.DownloadConnection;
import com.example.myokdownload.dowload.core.connection.DownloadUrlConnection;
import com.example.myokdownload.dowload.core.log.LogUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class Util {
    @Nullable
    public static String md5(String string) {
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ignored) {

        }

        if (hash != null) {
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b: hash) {
                if ((b & 0xFF) < 0x10) hex.append('0');
                hex.append(Integer.toHexString(b & 0xFF));
            }
            return hex.toString();
        }
        return null;
    }

    public static boolean isUriContentScheme(@NonNull Uri uri) {
        return uri.getScheme().equals(ContentResolver.SCHEME_CONTENT);
    }

    /**
     * @param si whether using SI unit refer to International System of Units.
     */
    public static String humanReadableBytes(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.ENGLISH, "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @SuppressLint("Range")
    public static long getSizeFromContentUri(@NonNull Uri contentUri) {
        final ContentResolver resolver = OKDownload.with().context().getContentResolver();
        final Cursor cursor = resolver.query(contentUri, null, null, null, null);
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                return cursor
                        .getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
            } finally {
                cursor.close();
            }
        }
        return 0;
    }

    public static void assembleBlock(@NonNull DownloadTask task, @NonNull BreakpointInfo info, long instanceLength, boolean isAcceptRange) {
        final int blockCount;
        if (OKDownload.with().downloadStrategy().isUseMultiBlock(isAcceptRange)) {
            blockCount = OKDownload.with().downloadStrategy().determineBlockCount(task, instanceLength);
        } else {
            blockCount = 1;
        }
        info.resetBlockInfos();
        final long eachLength = instanceLength / blockCount;
        long startOffset = 0;
        long contentLength = 0;
        for (int i = 0; i < blockCount; i++) {
            startOffset = startOffset + contentLength;
            if (i == 0) {
                final long remainLength = instanceLength % blockCount;
                contentLength = eachLength + remainLength;
            } else {
                contentLength = eachLength;
            }

            final BlockInfo blockInfo = new BlockInfo(startOffset, contentLength);
            info.addBlock(blockInfo);
        }
    }

    public static boolean isUriFileScheme(@NonNull Uri uri) {
        return uri.getScheme().equals(ContentResolver.SCHEME_FILE);
    }

    @SuppressLint("Range")
    @Nullable public static String getFilenameFromContentUri(@NonNull Uri contentUri) {
        final ContentResolver resolver = OKDownload.with().context().getContentResolver();
        final Cursor cursor = resolver.query(contentUri, null, null, null, null);
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                return cursor
                        .getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    @NonNull public static File getParentFile(final File file) {
        final File candidate = file.getParentFile();
        return candidate == null ? new File("/") : candidate;
    }

    public static long getFreeSpaceBytes(@NonNull StatFs statFs) {
        long freeSpaceBytes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            freeSpaceBytes = statFs.getAvailableBytes();
        } else {
            freeSpaceBytes = statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
        }
        return freeSpaceBytes;
    }

    public static void resetBlockIfDirty(BlockInfo info) {
        boolean isDirty = false;

        if (info.getCurrentOffset() < 0) {
            isDirty = true;
        } else if (info.getCurrentOffset() > info.getContentLength()) {
            isDirty = true;
        }

        if (isDirty) {
            LogUtil.w("resetBlockIfDirty", "block is dirty so have to reset: " + info);
            info.resetBlock();
        }
    }

    public static @NonNull DownloadStore createDefaultDatabase(Context context) {
        // You can import through com.liulishuo.okdownload:sqlite:{version}
        final String storeOnSqliteClassName
                = "com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnSQLite";

        try {
            final Constructor<?> constructor = Class.forName(storeOnSqliteClassName)
                    .getDeclaredConstructor(Context.class);
            return (DownloadStore) constructor.newInstance(context);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException ignored) {
        }

        return new BreakpointStoreOnCache();
    }

    public static @NonNull DownloadStore createRemitDatabase(@NonNull DownloadStore originStore) {
        DownloadStore finalStore = originStore;
        try {
            final Method createRemitSelf = originStore.getClass()
                    .getMethod("createRemitSelf");
            finalStore = (DownloadStore) createRemitSelf.invoke(originStore);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
        }

        LogUtil.d("Util", "Get final download store is " + finalStore);
        assert finalStore != null;
        return finalStore;
    }

    public static @NonNull DownloadConnection.Factory createDefaultConnectionFactory() {
        final String okhttpConnectionClassName
                = "com.liulishuo.okdownload.core.connection.DownloadOkHttp3Connection$Factory";
        try {
            final Constructor<?> constructor = Class.forName(okhttpConnectionClassName)
                    .getDeclaredConstructor();
            return (DownloadConnection.Factory) constructor.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | InvocationTargetException ignored) {
        }

        return new DownloadUrlConnection.Factory();
    }
}
