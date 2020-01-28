package io.legado.app.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.lang.reflect.Array
import java.util.*


object FileUtils {

    fun getFileByPath(filePath: String): File? {
        return if (filePath.isBlank()) null else File(filePath)
    }

    fun getSdCardPath(): String {
        var sdCardDirectory = Environment.getExternalStorageDirectory().absolutePath

        try {
            sdCardDirectory = File(sdCardDirectory).canonicalPath
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return sdCardDirectory
    }

    fun getStorageData(pContext: Context): ArrayList<String>? {

        val storageManager = pContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager

        try {
            val getVolumeList = storageManager.javaClass.getMethod("getVolumeList")

            val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getPath = storageVolumeClazz.getMethod("getPath")

            val invokeVolumeList = getVolumeList.invoke(storageManager) ?: return null
            val length = Array.getLength(invokeVolumeList)

            val list = ArrayList<String>()
            for (i in 0 until length) {
                val storageVolume = Array.get(invokeVolumeList, i)//得到StorageVolume对象
                val path = getPath.invoke(storageVolume) as String

                list.add(path)
            }
            return list
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }


    fun getExtSdCardPaths(con: Context): ArrayList<String> {
        val paths = ArrayList<String>()
        val files = ContextCompat.getExternalFilesDirs(con, "external")
        val firstFile = files[0]
        for (file in files) {
            if (file != null && file != firstFile) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index < 0) {
                    Log.w("", "Unexpected external file dir: " + file.absolutePath)
                } else {
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath
                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }

                    paths.add(path)
                }
            }
        }
        return paths
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
        selectionArgs: kotlin.Array<String>?
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
