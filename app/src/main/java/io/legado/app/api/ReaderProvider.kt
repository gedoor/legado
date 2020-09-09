/*
 * Copyright (C) 2020 w568w
 */
package io.legado.app.api

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import com.google.gson.Gson
import io.legado.app.web.controller.BookshelfController
import io.legado.app.web.controller.SourceController
import io.legado.app.web.utils.ReturnData
import java.util.*

/**
 * Export book data to other app.
 */
class ReaderProvider : ContentProvider() {
    private enum class RequestCode {
        SaveSource, SaveSources, SaveBook, DeleteSources, GetSource, GetSources, GetBookshelf, GetChapterList, GetBookContent
    }

    private val postBodyKey = "json"
    private val sMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            "${context?.applicationInfo?.packageName}.readerProvider".also { authority ->
                addURI(authority, "source/insert", RequestCode.SaveSource.ordinal)
                addURI(authority, "sources/insert", RequestCode.SaveSources.ordinal)
                addURI(authority, "book/insert", RequestCode.SaveBook.ordinal)
                addURI(authority, "sources/delete", RequestCode.DeleteSources.ordinal)
                addURI(authority, "source/query", RequestCode.GetSource.ordinal)
                addURI(authority, "sources/query", RequestCode.GetSources.ordinal)
                addURI(authority, "books/query", RequestCode.GetBookshelf.ordinal)
                addURI(authority, "book/chapter/query", RequestCode.GetChapterList.ordinal)
                addURI(authority, "book/content/query", RequestCode.GetBookContent.ordinal)
            }
        }
    }

    override fun onCreate(): Boolean {
        return false
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        if (sMatcher.match(uri) < 0) return -1
        when (RequestCode.values()[sMatcher.match(uri)]) {
            RequestCode.DeleteSources -> SourceController.deleteSources(selection)
            else -> throw IllegalStateException(
                "Unexpected value: " + RequestCode.values()[sMatcher.match(uri)].name
            )
        }
        return 0
    }

    override fun getType(uri: Uri): String? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (sMatcher.match(uri) < 0) return null
        when (RequestCode.values()[sMatcher.match(uri)]) {
            RequestCode.SaveSource -> values?.let {
                SourceController.saveSource(values.getAsString(postBodyKey))
            }
            RequestCode.SaveBook -> values?.let {
                BookshelfController.saveBook(values.getAsString(postBodyKey))
            }
            RequestCode.SaveSources -> values?.let {
                SourceController.saveSources(values.getAsString(postBodyKey))
            }
            else -> throw IllegalStateException(
                "Unexpected value: " + RequestCode.values()[sMatcher.match(uri)].name
            )
        }
        return null
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        val map: MutableMap<String, ArrayList<String>> = HashMap()
        uri.getQueryParameter("url")?.let {
            map["url"] = arrayListOf(it)
        }
        uri.getQueryParameter("index")?.let {
            map["index"] = arrayListOf(it)
        }
        return if (sMatcher.match(uri) < 0) null else when (RequestCode.values()[sMatcher.match(uri)]) {
            RequestCode.GetSource -> SimpleCursor(SourceController.getSource(map))
            RequestCode.GetSources -> SimpleCursor(SourceController.sources)
            RequestCode.GetBookshelf -> SimpleCursor(BookshelfController.bookshelf)
            RequestCode.GetBookContent -> SimpleCursor(BookshelfController.getBookContent(map))
            RequestCode.GetChapterList -> SimpleCursor(BookshelfController.getChapterList(map))
            else -> throw IllegalStateException(
                "Unexpected value: " + RequestCode.values()[sMatcher.match(uri)].name
            )
        }
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        throw UnsupportedOperationException("Not yet implemented")
    }

    /**
     * Simple inner class to deliver json callback data.
     *
     * Only getString() makes sense.
     */
    private class SimpleCursor(data: ReturnData?) : Cursor {

        private val mData: String = Gson().toJson(data)

        override fun getCount(): Int {
            return 1
        }

        override fun getPosition(): Int {
            return 0
        }

        override fun move(i: Int): Boolean {
            return true
        }

        override fun moveToPosition(i: Int): Boolean {
            return true
        }

        override fun moveToFirst(): Boolean {
            return true
        }

        override fun moveToLast(): Boolean {
            return true
        }

        override fun moveToNext(): Boolean {
            return true
        }

        override fun moveToPrevious(): Boolean {
            return true
        }

        override fun isFirst(): Boolean {
            return true
        }

        override fun isLast(): Boolean {
            return true
        }

        override fun isBeforeFirst(): Boolean {
            return false
        }

        override fun isAfterLast(): Boolean {
            return false
        }

        override fun getColumnIndex(s: String): Int {
            return 0
        }

        @Throws(IllegalArgumentException::class)
        override fun getColumnIndexOrThrow(s: String): Int {
            throw UnsupportedOperationException("Not yet implemented")
        }

        override fun getColumnName(i: Int): String? {
            return null
        }

        override fun getColumnNames(): Array<String> {
            return arrayOf()
        }

        override fun getColumnCount(): Int {
            return 0
        }

        override fun getBlob(i: Int): ByteArray {
            return ByteArray(0)
        }

        override fun getString(i: Int): String {
            return mData
        }

        override fun copyStringToBuffer(
            i: Int,
            charArrayBuffer: CharArrayBuffer
        ) {
        }

        override fun getShort(i: Int): Short {
            return 0
        }

        override fun getInt(i: Int): Int {
            return 0
        }

        override fun getLong(i: Int): Long {
            return 0
        }

        override fun getFloat(i: Int): Float {
            return 0f
        }

        override fun getDouble(i: Int): Double {
            return 0.0
        }

        override fun getType(i: Int): Int {
            return 0
        }

        override fun isNull(i: Int): Boolean {
            return false
        }

        override fun deactivate() {}
        override fun requery(): Boolean {
            return false
        }

        override fun close() {}
        override fun isClosed(): Boolean {
            return false
        }

        override fun registerContentObserver(contentObserver: ContentObserver) {}
        override fun unregisterContentObserver(contentObserver: ContentObserver) {}
        override fun registerDataSetObserver(dataSetObserver: DataSetObserver) {}
        override fun unregisterDataSetObserver(dataSetObserver: DataSetObserver) {}
        override fun setNotificationUri(contentResolver: ContentResolver, uri: Uri) {}

        override fun getNotificationUri(): Uri? {
            return null
        }

        override fun getWantsAllOnMoveCalls(): Boolean {
            return false
        }

        override fun setExtras(bundle: Bundle) {}
        override fun getExtras(): Bundle? {
            return null
        }

        override fun respond(bundle: Bundle): Bundle? {
            return null
        }

    }
}