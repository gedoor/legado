package io.legado.app.help

import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.AppConst
import io.legado.app.utils.FileDoc
import io.legado.app.utils.FileUtils.getMimeType
import splitties.init.appCtx
import java.io.File
import java.util.*

object BookMediaStore {
    private val DOWNLOAD_DIR =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    fun insertBook(doc: DocumentFile): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bookDetails = ContentValues().apply {
                put(MediaStore.Downloads.RELATIVE_PATH, "Download${File.separator}books")
                put(MediaStore.MediaColumns.DISPLAY_NAME, doc.name)
                put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(doc.name!!))
                put(MediaStore.MediaColumns.SIZE, doc.length())
            }
            appCtx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, bookDetails)
        } else {
            val destinyFile = File(DOWNLOAD_DIR, doc.name!!)
            FileProvider.getUriForFile(appCtx, AppConst.authority, destinyFile)
        }?.also { uri ->
            appCtx.contentResolver.openOutputStream(uri).use { outputStream ->
                val brr = ByteArray(1024)
                var len: Int
                val bufferedInputStream = appCtx.contentResolver.openInputStream(doc.uri)!!
                while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                    outputStream?.write(brr, 0, len)
                }
                outputStream?.flush()
                bufferedInputStream.close()
            }
        }
    }

    fun getBook(name: String): FileDoc? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED
            )
            val selection =
                "${MediaStore.Downloads.RELATIVE_PATH} like 'Download${File.separator}books${File.separator}%'"
            val sortOrder = "${MediaStore.Downloads.DISPLAY_NAME} ASC"
            appCtx.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                emptyArray(),
                sortOrder
            )?.use {
                val idColumn = it.getColumnIndex(projection[0])
                val nameColumn = it.getColumnIndex(projection[1])
                val sizeColumn = it.getColumnIndex(projection[2])
                val dateColumn = it.getColumnIndex(projection[3])
                if (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    return FileDoc(
                        name = it.getString(nameColumn),
                        isDir = false,
                        size = it.getLong(sizeColumn),
                        date = Date(it.getLong(dateColumn)),
                        uri = ContentUris.withAppendedId(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            id
                        )
                    )
                }
            }
        }

        return null
    }


}