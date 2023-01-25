package io.legado.app.help

import android.net.Uri
import androidx.annotation.Keep
import java.io.File

@Keep
object IntentType {

    fun from(uri: Uri): String? {
        return from(uri.toString())
    }

    fun from(file: File): String? {
        return from(file.absolutePath)
    }

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