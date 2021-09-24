package io.legado.app.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.nio.charset.Charset
import java.util.*


@Suppress("MemberVisibilityCanBePrivate")
object DocumentUtils {

    fun exists(root: DocumentFile, fileName: String, vararg subDirs: String): Boolean {
        val parent = getDirDocument(root, *subDirs) ?: return false
        return parent.findFile(fileName)?.exists() ?: false
    }

    fun delete(root: DocumentFile, fileName: String, vararg subDirs: String) {
        val parent: DocumentFile? = createFolderIfNotExist(root, *subDirs)
        parent?.findFile(fileName)?.delete()
    }

    fun createFileIfNotExist(
        root: DocumentFile,
        fileName: String,
        mimeType: String = "",
        vararg subDirs: String
    ): DocumentFile? {
        val parent: DocumentFile? = createFolderIfNotExist(root, *subDirs)
        return parent?.createFile(mimeType, fileName)
    }

    fun createFolderIfNotExist(root: DocumentFile, vararg subDirs: String): DocumentFile? {
        var parent: DocumentFile? = root
        for (subDirName in subDirs) {
            val subDir = parent?.findFile(subDirName)
                ?: parent?.createDirectory(subDirName)
            parent = subDir
        }
        return parent
    }

    fun getDirDocument(root: DocumentFile, vararg subDirs: String): DocumentFile? {
        var parent = root
        for (subDirName in subDirs) {
            val subDir = parent.findFile(subDirName)
            parent = subDir ?: return null
        }
        return parent
    }

    @JvmStatic
    @Throws(Exception::class)
    fun writeText(
        context: Context,
        data: String,
        fileUri: Uri,
        charset: Charset = Charsets.UTF_8
    ): Boolean {
        return writeBytes(context, data.toByteArray(charset), fileUri)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun writeBytes(context: Context, data: ByteArray, fileUri: Uri): Boolean {
        context.contentResolver.openOutputStream(fileUri)?.let {
            it.write(data)
            it.close()
            return true
        }
        return false
    }

    @JvmStatic
    @Throws(Exception::class)
    fun readText(context: Context, uri: Uri): String? {
        readBytes(context, uri)?.let {
            return String(it)
        }
        return null
    }

    @JvmStatic
    @Throws(Exception::class)
    fun readBytes(context: Context, uri: Uri): ByteArray? {
        context.contentResolver.openInputStream(uri)?.let {
            val len: Int = it.available()
            val buffer = ByteArray(len)
            it.read(buffer)
            it.close()
            return buffer
        }
        return null
    }

    fun listFiles(context: Context, uri: Uri): ArrayList<DocItem> {
        val docList = arrayListOf<DocItem>()
        var cursor: Cursor? = null
        try {
            val childrenUri = DocumentsContract
                .buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
            cursor = context.contentResolver.query(
                childrenUri, arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ), null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME
            )
            cursor?.let {
                val ici = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val mci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val dci = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (cursor.moveToFirst()) {
                    do {
                        val item = DocItem(
                            name = cursor.getString(nci),
                            attr = cursor.getString(mci),
                            size = cursor.getLong(sci),
                            date = Date(cursor.getLong(dci)),
                            uri = DocumentsContract
                                .buildDocumentUriUsingTree(uri, cursor.getString(ici))
                        )
                        docList.add(item)
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printOnDebug()
        } finally {
            cursor?.close()
        }
        return docList
    }

}

data class DocItem(
    val name: String,
    val attr: String,
    val size: Long,
    val date: Date,
    val uri: Uri
) {
    val isDir: Boolean by lazy {
        DocumentsContract.Document.MIME_TYPE_DIR == attr
    }

    val isContentPath get() = uri.isContentScheme()
}

@Throws(Exception::class)
fun DocumentFile.writeText(context: Context, data: String, charset: Charset = Charsets.UTF_8) {
    DocumentUtils.writeText(context, data, this.uri, charset)
}

@Throws(Exception::class)
fun DocumentFile.writeBytes(context: Context, data: ByteArray) {
    DocumentUtils.writeBytes(context, data, this.uri)
}

@Throws(Exception::class)
fun DocumentFile.readText(context: Context): String? {
    return DocumentUtils.readText(context, this.uri)
}

@Throws(Exception::class)
fun DocumentFile.readBytes(context: Context): ByteArray? {
    return DocumentUtils.readBytes(context, this.uri)
}
