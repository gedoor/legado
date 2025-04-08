package io.legado.app.utils

import android.net.Uri
import java.io.File

object IntentType {

    fun from(uri: Uri): String {
        return from(uri.toString())
    }

    fun from(file: File): String {
        return from(file.absolutePath)
    }

    fun from(path: String?): String {
        val suffix = path
            ?.substringAfterLast(File.separator)
            ?.substringAfterLast(".", "")
            ?.lowercase()
        return when (suffix) {
            "m4a", "mp3", "mid", "xmf", "ogg", "wav" -> "video/*"
            "3gp", "mp4" -> "audio/*"
            "jpg", "gif", "png", "jpeg", "bmp" -> "image/*"
            "", "txt", "json", "log" -> "text/plain"
            "apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }

}