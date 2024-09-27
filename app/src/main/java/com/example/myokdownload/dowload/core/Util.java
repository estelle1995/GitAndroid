package com.example.myokdownload.dowload.core;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
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
import com.example.myokdownload.dowload.core.connection.DownloadConnection;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

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

    @SuppressLint("Range")
    public static long getSizeFromContentUri(@NonNull Uri contentUri) {
        final ContentResolver resolver = OKDownload.with().context.getContentResolver();
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
        if (OKDownload.with().downloadStrategy.isUseMultiBlock(isAcceptRange)) {
            blockCount = OKDownload.with().downloadStrategy.determineBlockCount(task, instanceLength);
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

    public static long getFreeSpaceBytes(@NonNull StatFs statFs) {
        long freeSpaceBytes;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            freeSpaceBytes = statFs.getAvailableBytes();
        } else {
            freeSpaceBytes = statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
        }
        return freeSpaceBytes;
    }
}
