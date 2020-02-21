package io.legado.app.utils

import android.content.Context
import android.net.Uri
import java.io.File

@Throws(Exception::class)
fun Uri.readBytes(context: Context): ByteArray? {
    if (this.toString().isContentPath()) {
        return DocumentUtils.readBytes(context, this)
    } else {
        val path = RealPathUtil.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            return File(path).readBytes()
        }
    }
    return null
}

@Throws(Exception::class)
fun Uri.readText(context: Context): String? {
    if (this.toString().isContentPath()) {
        return DocumentUtils.readText(context, this)
    } else {
        val path = RealPathUtil.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            return File(path).readText()
        }
    }
    return null
}

@Throws(Exception::class)
fun Uri.writeBytes(context: Context, byteArray: ByteArray): Boolean {
    if (this.toString().isContentPath()) {
        return DocumentUtils.writeBytes(context, byteArray, this)
    } else {
        val path = RealPathUtil.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            File(path).writeBytes(byteArray)
            return true
        }
    }
    return false
}

@Throws(Exception::class)
fun Uri.writeText(context: Context, text: String): Boolean {
    if (this.toString().isContentPath()) {
        return DocumentUtils.writeText(context, text, this)
    } else {
        val path = RealPathUtil.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            File(path).writeText(text)
            return true
        }
    }
    return false
}