package io.legado.app.utils

import android.content.Context
import android.net.Uri
import java.io.File

fun Uri.isContentScheme() = this.scheme == "content"

@Throws(Exception::class)
fun Uri.readBytes(context: Context): ByteArray? {
    if (this.toString().isContentScheme()) {
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
    readBytes(context)?.let {
        return String(it)
    }
    return null
}

@Throws(Exception::class)
fun Uri.writeBytes(
    context: Context,
    byteArray: ByteArray
): Boolean {
    if (this.toString().isContentScheme()) {
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
    return writeBytes(context, text.toByteArray())
}
