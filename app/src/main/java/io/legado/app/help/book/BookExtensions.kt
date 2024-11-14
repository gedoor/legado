@file:Suppress("unused")

package io.legado.app.help.book

import android.net.Uri
import com.script.buildScriptBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.constant.AppLog
import io.legado.app.constant.BookSourceType
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseBook
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.FileDoc
import io.legado.app.utils.exists
import io.legado.app.utils.find
import io.legado.app.utils.inputStream
import io.legado.app.utils.isUri
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx
import java.io.File
import java.time.LocalDate
import java.time.Period.between
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min


val Book.isAudio: Boolean
    get() = isType(BookType.audio)

val Book.isImage: Boolean
    get() = isType(BookType.image)

val Book.isLocal: Boolean
    get() {
        if (type == 0) {
            return origin == BookType.localTag || origin.startsWith(BookType.webDavTag)
        }
        return isType(BookType.local)
    }

val Book.isLocalTxt: Boolean
    get() = isLocal && originName.endsWith(".txt", true)

val Book.isEpub: Boolean
    get() = isLocal && originName.endsWith(".epub", true)

val Book.isUmd: Boolean
    get() = isLocal && originName.endsWith(".umd", true)

val Book.isPdf: Boolean
    get() = isLocal && originName.endsWith(".pdf", true)

val Book.isMobi: Boolean
    get() = isLocal && (originName.endsWith(".mobi", true) ||
            originName.endsWith(".azw3", true) ||
            originName.endsWith(".azw", true))

val Book.isOnLineTxt: Boolean
    get() = !isLocal && isType(BookType.text)

val Book.isWebFile: Boolean
    get() = isType(BookType.webFile)

val Book.isUpError: Boolean
    get() = isType(BookType.updateError)

val Book.isArchive: Boolean
    get() = isType(BookType.archive)

val Book.isNotShelf: Boolean
    get() = isType(BookType.notShelf)

val Book.archiveName: String
    get() {
        if (!isArchive) throw NoStackTraceException("Book is not deCompressed from archive")
        // local_book::archive.rar
        // webDav::https://...../archive.rar
        return origin.substringAfter("::").substringAfterLast("/")
    }

fun Book.contains(word: String?): Boolean {
    if (word.isNullOrEmpty()) {
        return true
    }
    return name.contains(word)
            || author.contains(word)
            || originName.contains(word)
            || origin.contains(word)
            || kind?.contains(word) == true
            || intro?.contains(word) == true
}

private val localUriCache by lazy {
    ConcurrentHashMap<String, Uri>()
}

fun Book.getLocalUri(): Uri {
    if (!isLocal) {
        throw NoStackTraceException("不是本地书籍")
    }
    var uri = localUriCache[bookUrl]
    if (uri != null) {
        return uri
    }
    uri = if (bookUrl.isUri()) {
        Uri.parse(bookUrl)
    } else {
        Uri.fromFile(File(bookUrl))
    }
    //先检测uri是否有效,这个比较快
    uri.inputStream(appCtx).getOrNull()?.use {
        localUriCache[bookUrl] = uri
    }?.let {
        return uri
    }
    //不同的设备书籍保存路径可能不一样, uri无效时尝试寻找当前保存路径下的文件
    val defaultBookDir = AppConfig.defaultBookTreeUri
    val importBookDir = AppConfig.importBookPath

    // 查找书籍保存目录
    if (!defaultBookDir.isNullOrBlank()) {
        val treeUri = Uri.parse(defaultBookDir)
        val treeFileDoc = FileDoc.fromUri(treeUri, true)
        if (!treeFileDoc.exists()) {
            appCtx.toastOnUi("书籍保存目录失效，请重新设置！")
        } else {
            val fileDoc = treeFileDoc.find(originName, 5)
            if (fileDoc != null) {
                localUriCache[bookUrl] = fileDoc.uri
                //更新bookUrl 重启不用再找一遍
                bookUrl = fileDoc.toString()
                save()
                return fileDoc.uri
            }
        }
    }

    // 查找添加本地选择的目录
    if (!importBookDir.isNullOrBlank() && defaultBookDir != importBookDir) {
        val treeUri = if (importBookDir.isUri()) {
            Uri.parse(importBookDir)
        } else {
            Uri.fromFile(File(importBookDir))
        }
        val treeFileDoc = FileDoc.fromUri(treeUri, true)
        val fileDoc = treeFileDoc.find(originName, 5)
        if (fileDoc != null) {
            localUriCache[bookUrl] = fileDoc.uri
            bookUrl = fileDoc.toString()
            save()
            return fileDoc.uri
        }
    }

    localUriCache[bookUrl] = uri
    return uri
}


fun Book.getArchiveUri(): Uri? {
    val defaultBookDir = AppConfig.defaultBookTreeUri
    return if (isArchive && !defaultBookDir.isNullOrBlank()) {
        FileDoc.fromUri(Uri.parse(defaultBookDir), true)
            .find(archiveName)?.uri
    } else {
        null
    }
}

fun Book.cacheLocalUri(uri: Uri) {
    localUriCache[bookUrl] = uri
}

fun Book.removeLocalUriCache() {
    localUriCache.remove(bookUrl)
}

fun Book.getRemoteUrl(): String? {
    if (origin.startsWith(BookType.webDavTag)) {
        return origin.substring(BookType.webDavTag.length)
    }
    return null
}

fun Book.setType(@BookType.Type vararg types: Int) {
    type = 0
    addType(*types)
}

fun Book.addType(@BookType.Type vararg types: Int) {
    types.forEach {
        type = type or it
    }
}

fun Book.removeType(@BookType.Type vararg types: Int) {
    types.forEach {
        type = type and it.inv()
    }
}

fun Book.removeAllBookType() {
    removeType(BookType.allBookType)
}

fun Book.clearType() {
    type = 0
}

fun Book.isType(@BookType.Type bookType: Int): Boolean = type and bookType > 0

fun Book.upType() {
    if (type < 8) {
        type = when (type) {
            BookSourceType.image -> BookType.image
            BookSourceType.audio -> BookType.audio
            BookSourceType.file -> BookType.webFile
            else -> BookType.text
        }
        if (origin == BookType.localTag || origin.startsWith(BookType.webDavTag)) {
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

fun Book.sync(oldBook: Book) {
    val curBook = appDb.bookDao.getBook(oldBook.bookUrl)!!
    durChapterTime = curBook.durChapterTime
    durChapterIndex = curBook.durChapterIndex
    durChapterPos = curBook.durChapterPos
    durChapterTitle = curBook.durChapterTitle
    canUpdate = curBook.canUpdate
}

fun Book.getBookSource(): BookSource? {
    return appDb.bookSourceDao.getBookSource(origin)
}

fun Book.isLocalModified(): Boolean {
    return isLocal && LocalBook.getLastModified(this).getOrDefault(0L) > latestChapterTime
}

fun Book.releaseHtmlData() {
    infoHtml = null
    tocHtml = null
}

fun Book.isSameNameAuthor(other: Any?): Boolean {
    if (other is BaseBook) {
        return name == other.name && author == other.author
    }
    return false
}

fun Book.getExportFileName(suffix: String): String {
    val jsStr = AppConfig.bookExportFileName
    if (jsStr.isNullOrBlank()) {
        return "$name 作者：${getRealAuthor()}.$suffix"
    }
    val bindings = buildScriptBindings { bindings ->
        bindings["epubIndex"] = ""// 兼容老版本,修复可能存在的错误
        bindings["name"] = name
        bindings["author"] = getRealAuthor()
    }
    return kotlin.runCatching {
        RhinoScriptEngine.eval(jsStr, bindings).toString() + "." + suffix
    }.onFailure {
        AppLog.put("导出书名规则错误,使用默认规则\n${it.localizedMessage}", it)
    }.getOrDefault("$name 作者：${getRealAuthor()}.$suffix")
}

/**
 * 获取分割文件后的文件名
 */
fun Book.getExportFileName(
    suffix: String,
    epubIndex: Int,
    jsStr: String? = AppConfig.episodeExportFileName
): String {
    // 默认规则
    val default = "$name 作者：${getRealAuthor()} [${epubIndex}].$suffix"
    if (jsStr.isNullOrBlank()) {
        return default
    }
    val bindings = buildScriptBindings { bindings ->
        bindings["name"] = name
        bindings["author"] = getRealAuthor()
        bindings["epubIndex"] = epubIndex
    }
    return kotlin.runCatching {
        RhinoScriptEngine.eval(jsStr, bindings).toString() + "." + suffix
    }.onFailure {
        AppLog.put("导出书名规则错误,使用默认规则\n${it.localizedMessage}", it)
    }.getOrDefault(default)
}

// 根据当前日期计算章节总数
fun Book.simulatedTotalChapterNum(): Int {
    return if (readSimulating()) {
        val currentDate = LocalDate.now()
        val daysPassed = between(this.config.startDate, currentDate).days + 1
        // 计算当前应该解锁到哪一章
        val chaptersToUnlock =
            max(0, (config.startChapter ?: 0) + (daysPassed * config.dailyChapters))
        min(totalChapterNum, chaptersToUnlock)
    } else {
        totalChapterNum
    }
}

fun Book.readSimulating(): Boolean {
    return config.readSimulating
}

fun tryParesExportFileName(jsStr: String): Boolean {
    val bindings = buildScriptBindings { bindings ->
        bindings["name"] = "name"
        bindings["author"] = "author"
        bindings["epubIndex"] = "epubIndex"
    }
    return runCatching {
        RhinoScriptEngine.eval(jsStr, bindings)
        true
    }.getOrDefault(false)
}