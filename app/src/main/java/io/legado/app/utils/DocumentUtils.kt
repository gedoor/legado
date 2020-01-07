package io.legado.app.utils

import android.content.Context
import android.net.Uri

object DocumentUtils {

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