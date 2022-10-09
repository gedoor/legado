package io.legado.app.help.book

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.constant.BookSourceType
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.config.AppConfig.defaultBookTreeUri
import io.legado.app.exception.NoStackTraceException
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.getFile
import java.io.File
import splitties.init.appCtx


val Book.isAudio: Boolean
    get() {
        return type and BookType.audio > 0
    }

val Book.isImage: Boolean
    get() {
        return type and BookType.image > 0
    }

val Book.isLocal: Boolean
    get() {
        if (type == 0) {
            return origin == BookType.localTag || origin.startsWith(BookType.webDavTag)
        }
        return type and BookType.local > 0
    }

val Book.isLocalTxt: Boolean
    get() {
        return isLocal && originName.endsWith(".txt", true)
    }

val Book.isEpub: Boolean
    get() {
        return isLocal && originName.endsWith(".epub", true)
    }

val Book.isUmd: Boolean
    get() {
        return isLocal && originName.endsWith(".umd", true)
    }

val Book.isOnLineTxt: Boolean
    get() {
        return !isLocal && type and BookType.text > 0
    }

fun Book.getLocalUri(): Uri {
    if (isLocal) {
         val originBookUri = if (bookUrl.isContentScheme()) {
             Uri.parse(bookUrl)
         } else {
             Uri.fromFile(File(bookUrl))
         }
        //不同的设备书籍保存路径可能不一样 优先尝试寻找当前保存路径下的文件
        defaultBookTreeUri ?: return originBookUri
        val treeUri = Uri.parse(defaultBookTreeUri)
        return if (treeUri.isContentScheme()) {
            DocumentFile.fromTreeUri(appCtx, treeUri)?.run {
                findFile(originName)?.let {
                    if (it.exists()) it.uri else originBookUri
                } ?: originBookUri
            } ?: originBookUri
        } else {
            val treeFile = File(treeUri.path!!)
            val file = treeFile.getFile(originName)
            if (file.exists()) Uri.fromFile(file) else originBookUri
        }
    }
    throw NoStackTraceException("不是本地书籍")
}

fun Book.getRemoteUrl(): String? {
    if (origin.startsWith(BookType.webDavTag)) {
        return origin.substring(8)
    }
    return null
}

fun Book.upType() {
    if (type < 8) {
        type = when (type) {
            BookSourceType.image -> BookType.image
            BookSourceType.audio -> BookType.audio
            BookSourceType.file -> BookType.webFile
            else -> BookType.text
        }
        if (origin == "loc_book" || origin.startsWith(BookType.webDavTag)) {
            type = type or BookType.local
        }
    }
}

fun BookSource.getBookType(): Int {
    return when (bookSourceType) {
        BookSourceType.file -> BookType.text or BookType.webFile
        BookSourceType.image -> BookType.image
        BookSourceType.audio -> BookType.audio
        else -> BookType.text
    }
}
