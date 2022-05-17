package io.legado.app.ui.book.remote

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import kotlin.random.Random


abstract class RemoteBookManager {
    protected val remoteBookFolder : String = "books"
    protected val contentTypeList: ArrayList<String> = arrayListOf("epub","txt")
    abstract suspend fun initRemoteContext()
    abstract suspend fun getRemoteBookList(): MutableList<RemoteBook>
    abstract suspend fun upload(localBookUri: Uri): Boolean
    abstract suspend fun delete(remoteBookUrl: String): Boolean
    abstract suspend fun getRemoteBook(remoteBookUrl: String): RemoteBook
}