package io.legado.app.utils

import android.content.Context
import android.net.Uri

object DocumentUtils {

    fun writeText(context: Context, data: String, fileUri: Uri): Boolean {
        return writeBytes(context, data.toByteArray(), fileUri)
    }

    fun writeBytes(context: Context, data: ByteArray, fileUri: Uri): Boolean {
        try {
            context.contentResolver.openOutputStream(fileUri)?.let {
                it.write(data)
                it.close()
                return true
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun readText(context: Context, uri: Uri): String? {
        readBytes(context, uri)?.let {
            return String(it)
        }
        return null
    }

    fun readBytes(context: Context, uri: Uri): ByteArray? {
        try {
            context.contentResolver.openInputStream(uri)?.let {
                val len: Int = it.available()
                val buffer = ByteArray(len)
                it.read(buffer)
                it.close()
                return buffer
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


}