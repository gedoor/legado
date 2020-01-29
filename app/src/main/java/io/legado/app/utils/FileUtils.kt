package io.legado.app.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import io.legado.app.App
import java.io.File
import java.io.IOException


object FileUtils {

    fun exists(file: File, fileName: String, vararg subDirs: String): Boolean {
        val filePath =
            file.absolutePath + File.separator + subDirs.joinToString(File.separator) + File.separator + fileName
        return File(filePath).exists()
    }

    fun createFileIfNotExist(file: File, fileName: String, vararg subDirs: String): File {
        val filePath =
            file.absolutePath + File.separator + subDirs.joinToString(File.separator) + File.separator + fileName
        return createFileIfNotExist(filePath)
    }

    fun createFileIfNotExist(file: File, vararg subDirs: String): File {
        val filePath = file.absolutePath + File.separator + subDirs.joinToString(File.separator)
        return createFolderIfNotExist(filePath)
    }

    fun getCachePath(): String {
        return App.INSTANCE.externalCacheDir?.absolutePath
            ?: App.INSTANCE.cacheDir.absolutePath
    }

    fun createFolderIfNotExist(filePath: String): File {
        val file = File(filePath)
        //如果文件夹不存在，就创建它
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    @Synchronized
    fun createFileIfNotExist(filePath: String): File {
        val file = File(filePath)
        try {
            if (!file.exists()) {
                //创建父类文件夹
                file.parent?.let {
                    createFolderIfNotExist(it)
                }
                //创建文件
                file.createNewFile()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    //递归删除文件夹下的数据
    @Synchronized
    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        if (file.isDirectory) {
            val files = file.listFiles()
            files?.forEach { subFile ->
                val path = subFile.path
                deleteFile(path)
            }
        }
        //删除文件
        file.delete()
    }

    fun getSdCardPath(): String {
        @Suppress("DEPRECATION")
        var sdCardDirectory = Environment.getExternalStorageDirectory().absolutePath
        try {
            sdCardDirectory = File(sdCardDirectory).canonicalPath
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
        return sdCardDirectory
    }

    fun getPath(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    @Suppress("DEPRECATION")
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val split = id.split(":")
                val type = split[0]
                if ("raw".equals(type, ignoreCase = true)) {
                    //处理某些机型（比如Google Pixel ）ID是raw:/storage/emulated/0/Download/c20f8664da05ab6b4644913048ea8c83.mp4
                    return split[1]
                }

                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id)
                )

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex())

                val contentUri: Uri = when (split[0]) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> uri
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            return if (isGooglePhotosUri(uri))
                uri.lastPathSegment
            else
                getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)
        return null
    }

    private fun getDataColumn(
        context: Context, uri: Uri, selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        val column = "_data"
        val projection = arrayOf(column)

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

}
