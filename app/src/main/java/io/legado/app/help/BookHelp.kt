package io.legado.app.help

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.github.houbb.opencc4j.core.impl.ZhConvertBootstrap
import io.legado.app.App
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.model.localBook.AnalyzeTxtFile
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.apache.commons.text.similarity.JaccardSimilarity
import org.jetbrains.anko.toast
import java.io.File
import kotlin.math.min

object BookHelp {
    private const val cacheFolderName = "book_cache"
    val downloadPath: String
        get() = App.INSTANCE.getPrefString(PreferKey.downloadPath)
            ?: App.INSTANCE.getExternalFilesDir(null)?.absolutePath
            ?: App.INSTANCE.cacheDir.absolutePath

    private val downloadUri get() = Uri.parse(downloadPath)

    private fun bookFolderName(book: Book): String {
        return formatFolderName(book.name) + MD5Utils.md5Encode16(book.bookUrl)
    }

    fun formatChapterName(bookChapter: BookChapter): String {
        return String.format(
            "%05d-%s.nb",
            bookChapter.index,
            MD5Utils.md5Encode16(bookChapter.title)
        )
    }

    fun clearCache() {
        if (downloadPath.isContentPath()) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)
                ?.findFile(cacheFolderName)
                ?.delete()
        } else {
            FileUtils.deleteFile(
                FileUtils.getPath(
                    File(downloadPath),
                    subDirs = *arrayOf(cacheFolderName)
                )
            )
        }
    }

    @Synchronized
    fun saveContent(book: Book, bookChapter: BookChapter, content: String) {
        if (content.isEmpty()) return
        if (downloadPath.isContentPath()) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let { root ->
                DocumentUtils.createFileIfNotExist(
                    root,
                    formatChapterName(bookChapter),
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )?.uri?.writeText(App.INSTANCE, content)
            }
        } else {
            FileUtils.createFileIfNotExist(
                File(downloadPath),
                formatChapterName(bookChapter),
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).writeText(content)
        }
        postEvent(EventBus.SAVE_CONTENT, bookChapter)
    }

    fun getChapterFiles(book: Book): List<String> {
        val fileNameList = arrayListOf<String>()
        if (downloadPath.isContentPath()) {
            DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let { root ->
                DocumentUtils.createFolderIfNotExist(
                    root,
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )?.let { bookDoc ->
                    DocumentUtils.listFiles(App.INSTANCE, bookDoc.uri).forEach {
                        fileNameList.add(it.name)
                    }
                }
            }
        } else {
            FileUtils.createFolderIfNotExist(
                File(downloadPath),
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).list()?.let {
                fileNameList.addAll(it)
            }
        }
        return fileNameList
    }

    fun hasContent(book: Book, bookChapter: BookChapter): Boolean {
        when {
            book.isLocalBook() -> {
                return true
            }
            downloadPath.isContentPath() -> {
                DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let { root ->
                    return DocumentUtils.exists(
                        root,
                        formatChapterName(bookChapter),
                        subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                    )
                }
            }
            else -> {
                return FileUtils.exists(
                    File(downloadPath),
                    formatChapterName(bookChapter),
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )
            }
        }
        return false
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {
        when {
            book.isLocalBook() -> {
                return AnalyzeTxtFile.getContent(book, bookChapter)
            }
            downloadPath.isContentPath() -> {
                DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let { root ->
                    return DocumentUtils.getDirDocument(
                        root,
                        subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                    )?.findFile(formatChapterName(bookChapter))
                        ?.uri?.readText(App.INSTANCE)
                }
            }
            else -> {
                val file = FileUtils.getFile(
                    File(downloadPath),
                    formatChapterName(bookChapter),
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                )
                if (file.exists()) {
                    return file.readText()
                }
            }
        }
        return null
    }

    fun delContent(book: Book, bookChapter: BookChapter) {
        when {
            book.isLocalBook() -> return
            downloadPath.isContentPath() -> {
                DocumentFile.fromTreeUri(App.INSTANCE, downloadUri)?.let { root ->
                    DocumentUtils.getDirDocument(
                        root,
                        subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                    )?.findFile(formatChapterName(bookChapter))
                        ?.delete()
                }
            }
            else -> {
                FileUtils.createFileIfNotExist(
                    File(downloadPath),
                    formatChapterName(bookChapter),
                    subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
                ).delete()
            }
        }
    }

    private fun formatFolderName(folderName: String): String {
        return folderName.replace("[\\\\/:*?\"<>|.]".toRegex(), "")
    }

    fun formatAuthor(author: String?): String {
        return author
            ?.replace("作\\s*者[\\s:：]*".toRegex(), "")
            ?.replace("\\s+".toRegex(), " ")
            ?.trim { it <= ' ' }
            ?: ""
    }

    /**
     * 找到相似度最高的章节
     */
    fun getDurChapterIndexByChapterTitle(
        title: String?,
        index: Int,
        chapters: List<BookChapter>
    ): Int {
        if (title.isNullOrEmpty()) {
            return min(index, chapters.lastIndex)
        }
        if (chapters.size > index && title == chapters[index].title) {
            return index
        }

        var newIndex = 0
        val jSimilarity = JaccardSimilarity()
        var similarity = if (chapters.size > index) {
            jSimilarity.apply(title, chapters[index].title)
        } else 0.0
        if (similarity == 1.0) {
            return index
        } else {
            for (i in 1..50) {
                if (index - i in chapters.indices) {
                    jSimilarity.apply(title, chapters[index - i].title).let {
                        if (it > similarity) {
                            similarity = it
                            newIndex = index - i
                            if (similarity == 1.0) {
                                return newIndex
                            }
                        }
                    }
                }
                if (index + i in chapters.indices) {
                    jSimilarity.apply(title, chapters[index + i].title).let {
                        if (it > similarity) {
                            similarity = it
                            newIndex = index + i
                            if (similarity == 1.0) {
                                return newIndex
                            }
                        }
                    }
                }
            }
        }
        return newIndex
    }

    private var bookName: String? = null
    private var bookOrigin: String? = null
    private var replaceRules: List<ReplaceRule> = arrayListOf()

    @Synchronized
    fun upReplaceRules(name: String? = null, origin: String? = null) {
        if (name != null) {
            if (bookName != name || bookOrigin != origin) {
                replaceRules = if (origin.isNullOrEmpty()) {
                    App.db.replaceRuleDao().findEnabledByScope(name)
                } else {
                    App.db.replaceRuleDao().findEnabledByScope(name, origin)
                }
                bookName = name
                bookOrigin = origin
            }
        } else {
            val o = bookOrigin
            bookName?.let {
                replaceRules = if (o.isNullOrEmpty()) {
                    App.db.replaceRuleDao().findEnabledByScope(it)
                } else {
                    App.db.replaceRuleDao().findEnabledByScope(it, o)
                }
            }
        }
    }

    suspend fun disposeContent(
        title: String,
        name: String,
        origin: String?,
        content: String,
        enableReplace: Boolean
    ): String {
        var c = content
        if (enableReplace) {
            upReplaceRules(name, origin)
            replaceRules.forEach { item ->
                item.pattern.let {
                    if (it.isNotEmpty()) {
                        try {
                            c = if (item.isRegex) {
                                c.replace(it.toRegex(), item.replacement)
                            } else {
                                c.replace(it, item.replacement)
                            }
                        } catch (e: Exception) {
                            withContext(Main) {
                                App.INSTANCE.toast("${item.name}替换出错")
                            }
                        }
                    }
                }
            }
        }
        if (!c.substringBefore("\n").contains(title)) {
            c = "$title\n$c"
        }
        when (AppConfig.chineseConverterType) {
            1 -> c = ZhConvertBootstrap.newInstance().toSimple(c)
            2 -> c = ZhConvertBootstrap.newInstance().toTraditional(c)
        }
        return c.replace("\\s*\\n+\\s*".toRegex(), "\n${ReadBookConfig.bodyIndent}")
    }
}