package com.cwpdf.saver.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PdfDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "pdfs.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PDFS = "pdfs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_URI = "uri";
    public static final String COLUMN_KEY = "decryption_key";
    public static final String COLUMN_IS_ENCRYPTED = "is_encrypted";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_PDFS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_URL + " TEXT UNIQUE, " +
                    COLUMN_URI + " TEXT, " +
                    COLUMN_KEY + " TEXT, " +
                    COLUMN_IS_ENCRYPTED + " INTEGER, " +
                    COLUMN_TIMESTAMP + " INTEGER" +
                    ");";

    public PdfDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PDFS);
        onCreate(db);
    }
}
