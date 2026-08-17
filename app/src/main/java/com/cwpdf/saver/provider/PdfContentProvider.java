package com.cwpdf.saver.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cwpdf.saver.db.PdfDatabaseHelper;

public class PdfContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.cwpdf.saver.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PdfDatabaseHelper.TABLE_PDFS);

    private static final int PDFS = 1;
    private static final int PDF_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, PdfDatabaseHelper.TABLE_PDFS, PDFS);
        uriMatcher.addURI(AUTHORITY, PdfDatabaseHelper.TABLE_PDFS + "/#", PDF_ID);
    }

    private PdfDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new PdfDatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(PdfDatabaseHelper.TABLE_PDFS);

        switch (uriMatcher.match(uri)) {
            case PDFS:
                break;
            case PDF_ID:
                queryBuilder.appendWhere(PdfDatabaseHelper.COLUMN_ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case PDFS:
                return "vnd.android.cursor.dir/vnd.com.cwpdf.saver.pdfs";
            case PDF_ID:
                return "vnd.android.cursor.item/vnd.com.cwpdf.saver.pdfs";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // Use insertWithOnConflict to update existing URLs to prevent duplicates
        long id = db.insertWithOnConflict(PdfDatabaseHelper.TABLE_PDFS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (id > 0) {
            Uri itemUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(itemUri, null);
            return itemUri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriMatcher.match(uri)) {
            case PDFS:
                rowsDeleted = db.delete(PdfDatabaseHelper.TABLE_PDFS, selection, selectionArgs);
                break;
            case PDF_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = db.delete(PdfDatabaseHelper.TABLE_PDFS, PdfDatabaseHelper.COLUMN_ID + "=" + id, null);
                } else {
                    rowsDeleted = db.delete(PdfDatabaseHelper.TABLE_PDFS, PdfDatabaseHelper.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (uriMatcher.match(uri)) {
            case PDFS:
                rowsUpdated = db.update(PdfDatabaseHelper.TABLE_PDFS, values, selection, selectionArgs);
                break;
            case PDF_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = db.update(PdfDatabaseHelper.TABLE_PDFS, values, PdfDatabaseHelper.COLUMN_ID + "=" + id, null);
                } else {
                    rowsUpdated = db.update(PdfDatabaseHelper.TABLE_PDFS, values, PdfDatabaseHelper.COLUMN_ID + "=" + id + " and " + selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
