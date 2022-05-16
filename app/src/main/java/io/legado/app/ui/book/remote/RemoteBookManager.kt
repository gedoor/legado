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
    abstract suspend fun upload(localBookUrl: String): Boolean
    abstract suspend fun delete(remoteBookUrl: String): Boolean
    abstract suspend fun getRemoteBook(remoteBookUrl: String): RemoteBook

    /**
     * 把content uri转为 文件路径
     *
     * @param contentUri      要转换的content uri
     * @param contentResolver 解析器
     * @return
     */

    fun getFilePathFromContentUri(
        contentUri: Uri,
        contentResolver: ContentResolver
    ): String? {
        val filePath: String
        if (contentUri.scheme == ContentResolver.SCHEME_FILE)
            return File(requireNotNull(contentUri.path)).absolutePath
        else if(contentUri.scheme == ContentResolver.SCHEME_CONTENT){
            val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
            val cursor: Cursor? =
                contentResolver.query(contentUri, filePathColumn, null, null, null)
            cursor!!.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn[0])
            filePath = cursor.getString(columnIndex)
            cursor.close()
            return filePath
        }
        return null
    }


}