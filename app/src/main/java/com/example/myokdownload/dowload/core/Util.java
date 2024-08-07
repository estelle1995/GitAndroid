package com.example.myokdownload.dowload.core;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.OkDownload;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointStoreOnCache;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ThreadFactory;

public class Util {

    private static Logger logger = new EmptyLogger();

    public static final int CHUNKED_CONTENT_LENGTH = -1;
    public static boolean isUriFileScheme(@NonNull Uri uri) {
        return uri.getScheme().equals(ContentResolver.SCHEME_FILE);
    }

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static void w(String tag, String msg) {
        if (logger != null) {
            logger.w(tag, msg);
            return;
        }

        Log.w(tag, msg);
    }

    public static class EmptyLogger implements Logger {
        @Override public void e(String tag, String msg, Exception e) { }

        @Override public void w(String tag, String msg) { }

        @Override public void d(String tag, String msg) { }

        @Override public void i(String tag, String msg) { }
    }

    public interface Logger {
        void e(String tag, String msg, Exception e);

        void w(String tag, String msg);

        void d(String tag, String msg);

        void i(String tag, String msg);
    }

    @NonNull public static File getParentFile(final File file) {
        final File candidate = file.getParentFile();
        return candidate == null ? new File("/") : candidate;
    }

    public static @NonNull DownloadStore createDefaultDatabase(Context context) {
        // You can import through com.liulishuo.okdownload:sqlite:{version}
        final String storeOnSqliteClassName
                = "com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnSQLite";

        try {
            final Constructor constructor = Class.forName(storeOnSqliteClassName)
                    .getDeclaredConstructor(Context.class);
            return (DownloadStore) constructor.newInstance(context);
        } catch (ClassNotFoundException ignored) {
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (NoSuchMethodException ignored) {
        } catch (InvocationTargetException ignored) {
        }

        return new BreakpointStoreOnCache();
    }

    public static boolean isUriContentScheme(@NonNull Uri uri) {
        return uri.getScheme().equals(ContentResolver.SCHEME_CONTENT);
    }

    @SuppressLint("Range")
    @NonNull public static String getFilenameFromContentUri(@NonNull Uri contentUri) {
        final ContentResolver resolver = OkDownload.with().context().getContentResolver();
        final Cursor cursor = resolver.query(contentUri, null, null, null, null);
        if (cursor != null) {
            try {
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                final Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }
}
