package io.legado.app.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

fun Uri.isDocumentUri(context: Context): Boolean {
    return DocumentFile.isDocumentUri(context, this)
}

@Throws(Exception::class)
fun Uri.readBytes(context: Context): ByteArray? {
    if (DocumentFile.isDocumentUri(context, this)) {
        return DocumentUtils.readBytes(context, this)
    } else {
        val path = FileUtils.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            return File(path).readBytes()
        }
    }
    return null
}

@Throws(Exception::class)
fun Uri.readText(context: Context): String? {
    if (DocumentFile.isDocumentUri(context, this)) {
        return DocumentUtils.readText(context, this)
    } else {
        val path = FileUtils.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            return File(path).readText()
        }
    }
    return null
}

@Throws(Exception::class)
fun Uri.writeBytes(context: Context, byteArray: ByteArray): Boolean {
    if (DocumentFile.isDocumentUri(context, this)) {
        return DocumentUtils.writeBytes(context, byteArray, this)
    } else {
        val path = FileUtils.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            File(path).writeBytes(byteArray)
            return true
        }
    }
    return false
}

@Throws(Exception::class)
fun Uri.writeText(context: Context, text: String): Boolean {
    if (DocumentFile.isDocumentUri(context, this)) {
        return DocumentUtils.writeText(context, text, this)
    } else {
        val path = FileUtils.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            File(path).writeText(text)
            return true
        }
    }
    return false
}