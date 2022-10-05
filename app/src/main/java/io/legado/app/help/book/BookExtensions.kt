package io.legado.app.help.book

import android.net.Uri
import io.legado.app.constant.BookSourceType
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.utils.isContentScheme
import java.io.File


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
        if (bookUrl.isContentScheme()) {
            return Uri.parse(bookUrl)
        }
        return Uri.fromFile(File(bookUrl))
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
