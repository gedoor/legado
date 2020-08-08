/*
 * Copyright (C) 2020 w568w
 */

package io.legado.app.api;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.legado.app.web.controller.BookshelfController;
import io.legado.app.web.controller.SourceController;
import io.legado.app.web.utils.ReturnData;

/**
 * Export book data to other app.
 */
public class ReaderProvider extends ContentProvider {
    private enum RequestCode {
        saveSource, saveSources, saveBook, deleteSources, getSource, getSources, getBookshelf, getChapterList, getBookContent
    }

    public static final String POST_BODY_KEY = "json";
    public static final String AUTHORITY = "io.legado.app.api.ReaderProvider";
    private static UriMatcher sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private SourceController mSourceController;
    private BookshelfController mBookshelfController;

    static {
        sMatcher.addURI(AUTHORITY, "source/insert", RequestCode.saveSource.ordinal());
        sMatcher.addURI(AUTHORITY, "sources/insert", RequestCode.saveSources.ordinal());
        sMatcher.addURI(AUTHORITY, "book/insert", RequestCode.saveBook.ordinal());
        sMatcher.addURI(AUTHORITY, "sources/delete", RequestCode.deleteSources.ordinal());
        sMatcher.addURI(AUTHORITY, "source/query", RequestCode.getSource.ordinal());
        sMatcher.addURI(AUTHORITY, "sources/query", RequestCode.getSources.ordinal());
        sMatcher.addURI(AUTHORITY, "books/query", RequestCode.getBookshelf.ordinal());
        sMatcher.addURI(AUTHORITY, "book/chapter/query", RequestCode.getChapterList.ordinal());
        sMatcher.addURI(AUTHORITY, "book/content/query", RequestCode.getBookContent.ordinal());
    }

    public ReaderProvider() {
    }

    @Override
    public int delete(@NotNull Uri uri, String selection, String[] selectionArgs) {
        if (sMatcher.match(uri) < 0)
            return -1;
        switch (RequestCode.values()[sMatcher.match(uri)]) {
            case deleteSources:
                mSourceController.deleteSources(selection);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + RequestCode.values()[sMatcher.match(uri)].name());
        }
        return 0;
    }

    @Override
    public String getType(@NotNull Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(@NotNull Uri uri, ContentValues values) {
        if (sMatcher.match(uri) < 0)
            return null;
        switch (RequestCode.values()[sMatcher.match(uri)]) {
            case saveSource:
                mSourceController.saveSource(values.getAsString(POST_BODY_KEY));
                break;
            case saveBook:
                mBookshelfController.saveBook(values.getAsString(POST_BODY_KEY));
            case saveSources:
                mSourceController.saveSources(values.getAsString(POST_BODY_KEY));
            default:
                throw new IllegalStateException("Unexpected value: " + RequestCode.values()[sMatcher.match(uri)].name());
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        mSourceController = new SourceController();
        mBookshelfController = new BookshelfController();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Map<String, ArrayList<String>> map = new HashMap<>();

        ArrayList<String> list = new ArrayList<>();
        list.add(uri.getQueryParameter("url"));
        map.put("url", list);

        list = new ArrayList<>();
        list.add(uri.getQueryParameter("index"));
        map.put("index", list);

        if (sMatcher.match(uri) < 0)
            return null;
        switch (RequestCode.values()[sMatcher.match(uri)]) {
            case getSource:
                return new SimpleCursor(mSourceController.getSource(map));
            case getSources:
                return new SimpleCursor(mSourceController.getSources());
            case getBookshelf:
                return new SimpleCursor(mBookshelfController.getBookshelf());
            case getBookContent:
                return new SimpleCursor(mBookshelfController.getBookContent(map));
            case getChapterList:
                return new SimpleCursor(mBookshelfController.getChapterList(map));
            default:
                throw new IllegalStateException("Unexpected value: " + RequestCode.values()[sMatcher.match(uri)].name());
        }
    }

    @Override
    public int update(@NotNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Simple inner class to deliver json callback data.
     *
     * Only getString() makes sense.
     */
    private static class SimpleCursor implements Cursor {
        private String mData;

        public SimpleCursor(ReturnData data) {
            this.mData = new Gson().toJson(data);
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public int getPosition() {
            return 0;
        }

        @Override
        public boolean move(int i) {
            return true;
        }

        @Override
        public boolean moveToPosition(int i) {
            return true;
        }

        @Override
        public boolean moveToFirst() {
            return true;
        }

        @Override
        public boolean moveToLast() {
            return true;
        }

        @Override
        public boolean moveToNext() {
            return true;
        }

        @Override
        public boolean moveToPrevious() {
            return true;
        }

        @Override
        public boolean isFirst() {
            return true;
        }

        @Override
        public boolean isLast() {
            return true;
        }

        @Override
        public boolean isBeforeFirst() {
            return false;
        }

        @Override
        public boolean isAfterLast() {
            return false;
        }

        @Override
        public int getColumnIndex(String s) {
            return 0;
        }

        @Override
        public int getColumnIndexOrThrow(String s) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        @Override
        public String getColumnName(int i) {
            return null;
        }

        @Override
        public String[] getColumnNames() {
            return new String[0];
        }

        @Override
        public int getColumnCount() {
            return 0;
        }

        @Override
        public byte[] getBlob(int i) {
            return new byte[0];
        }

        @Override
        public String getString(int i) {
            return mData;
        }

        @Override
        public void copyStringToBuffer(int i, CharArrayBuffer charArrayBuffer) {

        }

        @Override
        public short getShort(int i) {
            return 0;
        }

        @Override
        public int getInt(int i) {
            return 0;
        }

        @Override
        public long getLong(int i) {
            return 0;
        }

        @Override
        public float getFloat(int i) {
            return 0;
        }

        @Override
        public double getDouble(int i) {
            return 0;
        }

        @Override
        public int getType(int i) {
            return 0;
        }

        @Override
        public boolean isNull(int i) {
            return false;
        }

        @Override
        public void deactivate() {

        }

        @Override
        public boolean requery() {
            return false;
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void registerContentObserver(ContentObserver contentObserver) {

        }

        @Override
        public void unregisterContentObserver(ContentObserver contentObserver) {

        }

        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

        }

        @Override
        public void setNotificationUri(ContentResolver contentResolver, Uri uri) {

        }

        @Override
        public Uri getNotificationUri() {
            return null;
        }

        @Override
        public boolean getWantsAllOnMoveCalls() {
            return false;
        }

        @Override
        public void setExtras(Bundle bundle) {

        }

        @Override
        public Bundle getExtras() {
            return null;
        }

        @Override
        public Bundle respond(Bundle bundle) {
            return null;
        }
    }
}
