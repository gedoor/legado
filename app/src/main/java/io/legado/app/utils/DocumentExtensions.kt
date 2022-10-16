package io.legado.app.utils

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import io.legado.app.exception.NoStackTraceException
import splitties.init.appCtx
import splitties.systemservices.downloadManager
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

    private val projection by lazy {
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
    }

    /**
     * DocumentFile 的 listFiles() 非常的慢,所以这里直接从数据库查询
     */
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
                childrenUri, projection, null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME
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

        fun fromUri(uri: Uri, isDir: Boolean): FileDoc {
            if (uri.isContentScheme()) {
                val doc = if (isDir) {
                    DocumentFile.fromTreeUri(appCtx, uri)!!
                } else if (uri.host == "downloads") {
                    val query = DownloadManager.Query()
                    query.setFilterById(uri.lastPathSegment!!.toLong())
                    downloadManager.query(query).use {
                        if (it.moveToFirst()) {
                            val lUriColum = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val lUri = it.getString(lUriColum)
                            DocumentFile.fromSingleUri(appCtx, Uri.parse(lUri))!!
                        } else {
                            DocumentFile.fromSingleUri(appCtx, uri)!!
                        }
                    }
                } else {
                    DocumentFile.fromSingleUri(appCtx, uri)!!
                }
                return FileDoc(doc.name ?: "", true, doc.length(), doc.lastModified(), doc.uri)
            }
            val file = File(uri.path!!)
            return FileDoc(file.name, isDir, file.length(), file.lastModified(), uri)
        }

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

fun FileDoc.list(filter: ((file: FileDoc) -> Boolean)? = null): ArrayList<FileDoc>? {
    if (isDir) {
        return DocumentUtils.listFiles(uri, filter)
    }
    return null
}

fun FileDoc.find(name: String): FileDoc? {
    return list {
        it.name == name
    }?.firstOrNull()
}

@Throws(Exception::class)
fun DocumentFile.writeText(context: Context, data: String, charset: Charset = Charsets.UTF_8) {
    uri.writeText(context, data, charset)
}

@Throws(Exception::class)
fun DocumentFile.writeBytes(context: Context, data: ByteArray) {
    uri.writeBytes(context, data)
}

@Throws(Exception::class)
fun DocumentFile.readText(context: Context): String {
    return String(readBytes(context))
}

@Throws(Exception::class)
fun DocumentFile.readBytes(context: Context): ByteArray {
    return context.contentResolver.openInputStream(uri)?.let {
        val len: Int = it.available()
        val buffer = ByteArray(len)
        it.read(buffer)
        it.close()
        return buffer
    } ?: throw NoStackTraceException("打开文件失败\n${uri}")
}
