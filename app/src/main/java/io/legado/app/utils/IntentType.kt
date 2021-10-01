package io.legado.app.utils

import android.net.Uri
import java.io.File

object IntentType {

    @JvmStatic
    fun from(uri: Uri): String? {
        return from(uri.toString())
    }

    @JvmStatic
    fun from(file: File): String? {
        return from(file.absolutePath)
    }

    @JvmStatic
    fun from(path: String?): String? {
        return when (path?.substringAfterLast(".")?.lowercase()) {
            "apk" -> "application/vnd.android.package-archive"
            "m4a", "mp3", "mid", "xmf", "ogg", "wav" -> "video/*"
            "3gp", "mp4" -> "audio/*"
            "jpg", "gif", "png", "jpeg", "bmp" -> "image/*"
            "txt", "json" -> "text/plain"
            else -> null
        }
    }

}