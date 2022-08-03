package io.legado.app.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import io.legado.app.exception.NoStackTraceException
import splitties.init.appCtx
import java.io.File
import java.nio.charset.Charset


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
        return parent?.findFile(fileName) ?: parent?.createFile(mimeType, fileName)
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
    fun readText(context: Context, uri: Uri): String {
        return String(readBytes(context, uri))
    }

    @JvmStatic
    @Throws(Exception::class)
    fun readBytes(context: Context, uri: Uri): ByteArray {
        context.contentResolver.openInputStream(uri)?.let {
            val len: Int = it.available()
            val buffer = ByteArray(len)
            it.read(buffer)
            it.close()
            return buffer
        } ?: throw NoStackTraceException("打开文件失败\n${uri}")
    }

    @Throws(Exception::class)
    fun listFiles(uri: Uri, filter: ((file: FileDoc) -> Boolean)? = null): ArrayList<FileDoc> {
        if (!uri.isContentScheme()) {
            return listFiles(uri.path!!, filter)
        }
        val childrenUri = DocumentsContract
            .buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
        val docList = arrayListOf<FileDoc>()
        var cursor: Cursor? = null
        try {
            cursor = appCtx.contentResolver.query(
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
                        val item = FileDoc(
                            name = cursor.getString(nci),
                            isDir = cursor.getString(mci) == DocumentsContract.Document.MIME_TYPE_DIR,
                            size = cursor.getLong(sci),
                            lastModified = cursor.getLong(dci),
                            uri = DocumentsContract
                                .buildDocumentUriUsingTree(uri, cursor.getString(ici))
                        )
                        if (filter == null || filter.invoke(item)) {
                            docList.add(item)
                        }
                    } while (cursor.moveToNext())
                }
            }
        } finally {
            cursor?.close()
        }
        return docList
    }

    @Throws(Exception::class)
    fun listFiles(path: String, filter: ((file: FileDoc) -> Boolean)? = null): ArrayList<FileDoc> {
        val docList = arrayListOf<FileDoc>()
        val file = File(path)
        file.listFiles()?.forEach {
            val item = FileDoc(
                it.name,
                it.isDirectory,
                it.length(),
                it.lastModified(),
                Uri.fromFile(it)
            )
            if (filter == null || filter.invoke(item)) {
                docList.add(item)
            }
        }
        return docList
    }

}

data class FileDoc(
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val uri: Uri
) {

    override fun toString(): String {
        return if (uri.isContentScheme()) uri.toString() else uri.path!!
    }

    val isContentScheme get() = uri.isContentScheme()

    fun readBytes(): ByteArray {
        return uri.readBytes(appCtx)
    }

    companion object {

        fun fromDocumentFile(doc: DocumentFile): FileDoc {
            return FileDoc(
                name = doc.name ?: "",
                isDir = doc.isDirectory,
                size = doc.length(),
                lastModified = doc.lastModified(),
                uri = doc.uri
            )
        }

        fun fromFile(file: File): FileDoc {
            return FileDoc(
                name = file.name,
                isDir = file.isDirectory,
                size = file.length(),
                lastModified = file.lastModified(),
                uri = Uri.fromFile(file)
            )
        }

    }
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
fun DocumentFile.readText(context: Context): String {
    return DocumentUtils.readText(context, this.uri)
}

@Throws(Exception::class)
fun DocumentFile.readBytes(context: Context): ByteArray {
    return DocumentUtils.readBytes(context, this.uri)
}
