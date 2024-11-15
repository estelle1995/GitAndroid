package com.example.myokdownload.sqlite.breakpoint;

import static com.example.myokdownload.sqlite.breakpoint.BreakpointSQLiteKey.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class BreakpointSQLiteHelper extends SQLiteOpenHelper {
    private static final String NAME = "okdownload-breakpoint.db";
    private static final int VERSION = 3;

    private static final String RESPONSE_FILENAME_TABLE_NAME = "okdownloadResponseFilename";
    private static final String BREAKPOINT_TABLE_NAME = "breakpoint";
    private static final String BLOCK_TABLE_NAME = "block";
    static final String TASK_FILE_DIRTY_TABLE_NAME = "taskFileDirty";

    public BreakpointSQLiteHelper(Context context) {
        super(context, NAME, null, VERSION);
    }


    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
        } else {
            db.enableWriteAheadLogging();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CreateTable.BREAKPOINT);
        db.execSQL(CreateTable.BLOCK);
        db.execSQL(CreateTable.RESPONSE_FILENAME);
        db.execSQL(CreateTable.TASK_FILE_DIRTY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL(Migration.RESPONSE_FILENAME_1_TO_2);
        }
        if (oldVersion <= 2) {
            db.execSQL(Migration.TASK_FILE_DIRTY_NONE_TO_2);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void markFileDirty(int id) {
        final SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues(1);
        values.put(ID, id);
        db.insert(TASK_FILE_DIRTY_TABLE_NAME, null, values);
    }

    public void markFileClear(int id) {
        getWritableDatabase().delete(TASK_FILE_DIRTY_TABLE_NAME, ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    public List<Integer> loadDirtyFileList() {
        final List<Integer> dirtyFileList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getWritableDatabase().rawQuery(Select.ALL_FROM_TASK_FILE_DIRTY,
                    null);
            while (cursor.moveToNext()) {
                dirtyFileList.add(cursor.getInt(cursor.getColumnIndex(ID)));
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return dirtyFileList;
    }

    private interface CreateTable {
        static final String BREAKPOINT = "CREATE TABLE IF NOT EXISTS "
                + BREAKPOINT_TABLE_NAME + "( "
                + ID + " INTEGER PRIMARY KEY, "
                + URL + " VARCHAR NOT NULL, "
                + ETAG + " VARCHAR, "
                + PARENT_PATH + " VARCHAR NOT NULL, "
                + FILENAME + " VARCHAR, "
                + TASK_ONLY_PARENT_PATH + " TINYINT(1) DEFAULT 0, "
                + CHUNKED + " TINYINT(1) DEFAULT 0)";

        static final String BLOCK = "CREATE TABLE IF NOT EXISTS "
                + BLOCK_TABLE_NAME + "( "
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HOST_ID + " INTEGER, "
                + BLOCK_INDEX + " INTEGER, "
                + START_OFFSET + " INTEGER, "
                + CONTENT_LENGTH + " INTEGER, "
                + CURRENT_OFFSET + " INTEGER)";

        static final String RESPONSE_FILENAME = "CREATE TABLE IF NOT EXISTS "
                + RESPONSE_FILENAME_TABLE_NAME + "( "
                + URL + " VARCHAR NOT NULL PRIMARY KEY, "
                + FILENAME + " VARCHAR NOT NULL)";

        static final String TASK_FILE_DIRTY = "CREATE TABLE IF NOT EXISTS "
                + TASK_FILE_DIRTY_TABLE_NAME + "( "
                + ID + " INTEGER PRIMARY KEY)";
    }

    private interface Migration {
        static final String RESPONSE_FILENAME_1_TO_2 = CreateTable.RESPONSE_FILENAME;
        static final String TASK_FILE_DIRTY_NONE_TO_2 = CreateTable.TASK_FILE_DIRTY;
    }

    private interface Select {
        static final String ALL_FROM_TASK_FILE_DIRTY = "SELECT * FROM " + TASK_FILE_DIRTY_TABLE_NAME;
        static final String ALL_FROM_BREAKPOINT = "SELECT * FROM " + BREAKPOINT_TABLE_NAME;
        static final String ALL_FROM_BLOCK = "SELECT * FROM " + BLOCK_TABLE_NAME;
        static final String ALL_FROM_RESPONSE_FILENAME = "SELECT * FROM " + RESPONSE_FILENAME_TABLE_NAME;
        static final String FILENAME_FROM_RESPONSE_FILENAME_BY_URL = "SELECT " + FILENAME + " FROM " + RESPONSE_FILENAME_TABLE_NAME
                + " WHERE " + URL + " = ?";
        static final String ID_FROM_BREAKPOINT_BY_ID = "SELECT " + ID + " FROM " + BREAKPOINT_TABLE_NAME + " WHERE " + ID + " = ? LIMIT 1";
    }
}
