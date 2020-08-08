package io.legado.app.help

import com.hankcs.hanlp.HanLP
import io.legado.app.App
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.FileUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.externalFilesDir
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.apache.commons.text.similarity.JaccardSimilarity
import org.jetbrains.anko.toast
import java.io.File
import kotlin.math.min

object BookHelp {
    private const val cacheFolderName = "book_cache"
    private val downloadDir: File = App.INSTANCE.externalFilesDir

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
        FileUtils.deleteFile(
            FileUtils.getPath(
                downloadDir,
                subDirs = *arrayOf(cacheFolderName)
            )
        )
    }

    @Synchronized
    fun saveContent(book: Book, bookChapter: BookChapter, content: String) {
        if (content.isEmpty()) return
        FileUtils.createFileIfNotExist(
            downloadDir,
            formatChapterName(bookChapter),
            subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
        ).writeText(content)
        postEvent(EventBus.SAVE_CONTENT, bookChapter)
    }

    fun getChapterFiles(book: Book): List<String> {
        val fileNameList = arrayListOf<String>()
        FileUtils.createFolderIfNotExist(
            downloadDir,
            subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
        ).list()?.let {
            fileNameList.addAll(it)
        }
        return fileNameList
    }

    fun hasContent(book: Book, bookChapter: BookChapter): Boolean {
        return if (book.isLocalBook()) {
            true
        } else {
            FileUtils.exists(
                downloadDir,
                formatChapterName(bookChapter),
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            )
        }
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {
        if (book.isLocalBook()) {
            return LocalBook.getContext(book, bookChapter)
        } else {
            val file = FileUtils.getFile(
                downloadDir,
                formatChapterName(bookChapter),
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            )
            if (file.exists()) {
                return file.readText()
            }
        }
        return null
    }

    fun delContent(book: Book, bookChapter: BookChapter) {
        if (book.isLocalBook()) {
            return
        } else {
            FileUtils.createFileIfNotExist(
                downloadDir,
                formatChapterName(bookChapter),
                subDirs = *arrayOf(cacheFolderName, bookFolderName(book))
            ).delete()
        }
    }

    private fun formatFolderName(folderName: String): String {
        return folderName.replace("[\\\\/:*?\"<>|.]".toRegex(), "")
    }

    fun formatBookName(name: String): String {
        return name
            .replace("\\s+作\\s*者.*".toRegex(), "")
            .trim { it <= ' ' }
    }

    fun formatBookAuthor(author: String): String {
        return author
            .replace(".*?作\\s*?者[:：]".toRegex(), "")
            .trim { it <= ' ' }
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
    suspend fun upReplaceRules() {
        withContext(IO) {
            synchronized(this) {
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
    }

    suspend fun disposeContent(
        title: String,
        name: String,
        origin: String?,
        content: String,
        enableReplace: Boolean
    ): List<String> {
        var c = content
        if (enableReplace) {
            synchronized(this) {
                if (bookName != name || bookOrigin != origin) {
                    bookName = name
                    bookOrigin = origin
                    replaceRules = if (origin.isNullOrEmpty()) {
                        App.db.replaceRuleDao().findEnabledByScope(name)
                    } else {
                        App.db.replaceRuleDao().findEnabledByScope(name, origin)
                    }
                }
            }
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
        try {
            when (AppConfig.chineseConverterType) {
                1 -> c = HanLP.convertToSimplifiedChinese(c)
                2 -> c = HanLP.convertToTraditionalChinese(c)
            }
        } catch (e: Exception) {
            withContext(Main) {
                App.INSTANCE.toast("简繁转换出错")
            }
        }
        val contents = arrayListOf<String>()
        c.split("\n").forEach {
            val str = it.replace("^\\s+".toRegex(), "")
                .replace("\r", "")
            if (contents.isEmpty()) {
                contents.add(title)
                if (str != title && it.isNotEmpty()) {
                    contents.add("${ReadBookConfig.bodyIndent}$str")
                }
            } else if (str.isNotEmpty()) {
                contents.add("${ReadBookConfig.bodyIndent}$str")
            }
        }
        return contents
    }
}