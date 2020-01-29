package io.legado.app.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object DocumentUtils {

    fun exists(root: DocumentFile, fileName: String, vararg subDirs: String): Boolean {
        val parent = getDirDocument(root, *subDirs) ?: return false
        return parent.findFile(fileName)?.exists() ?: false
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
    fun writeText(context: Context, data: String, fileUri: Uri): Boolean {
        return writeBytes(context, data.toByteArray(), fileUri)
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


}