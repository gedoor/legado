package io.legado.app.help

import com.hankcs.hanlp.HanLP
import io.legado.app.App
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import org.apache.commons.text.similarity.JaccardSimilarity
import org.jetbrains.anko.toast
import java.io.File
import kotlin.math.min

object BookHelp {
    private const val cacheFolderName = "book_cache"
    private const val cacheImageFolderName = "images"
    private val downloadDir: File = App.INSTANCE.externalFilesDir

    fun formatChapterName(bookChapter: BookChapter): String {
        return String.format(
            "%05d-%s.nb",
            bookChapter.index,
            MD5Utils.md5Encode16(bookChapter.title)
        )
    }

    fun clearCache() {
        FileUtils.deleteFile(
            FileUtils.getPath(downloadDir, cacheFolderName)
        )
    }

    fun clearCache(book: Book) {
        val filePath = FileUtils.getPath(downloadDir, cacheFolderName, book.getFolderName())
        FileUtils.deleteFile(filePath)
    }

    /**
     * 清楚已删除书的缓存
     */
    fun clearRemovedCache() {
        Coroutine.async {
            val bookFolderNames = arrayListOf<String>()
            App.db.bookDao().all.forEach {
                bookFolderNames.add(it.getFolderName())
            }
            val file = FileUtils.getDirFile(downloadDir, cacheFolderName)
            file.listFiles()?.forEach { bookFile ->
                if (!bookFolderNames.contains(bookFile.name)) {
                    FileUtils.deleteFile(bookFile.absolutePath)
                }
            }
        }
    }

    fun saveContent(book: Book, bookChapter: BookChapter, content: String) {
        if (content.isEmpty()) return
        //保存文本
        FileUtils.createFileIfNotExist(
            downloadDir,
            cacheFolderName,
            book.getFolderName(),
            formatChapterName(bookChapter),
        ).writeText(content)
        //保存图片
        content.split("\n").forEach {
            val matcher = AppPattern.imgPattern.matcher(it)
            if (matcher.find()) {
                var src = matcher.group(1)
                src = NetworkUtils.getAbsoluteURL(bookChapter.url, src)
                src?.let {
                    saveImage(book, src)
                }
            }
        }
        postEvent(EventBus.SAVE_CONTENT, bookChapter)
    }

    fun saveImage(book: Book, src: String) {
        val analyzeUrl = AnalyzeUrl(src)
        analyzeUrl.getImageBytes(book.origin)?.let {
            FileUtils.createFileIfNotExist(
                downloadDir,
                cacheFolderName,
                book.getFolderName(),
                cacheImageFolderName,
                "${MD5Utils.md5Encode16(src)}${getImageSuffix(src)}"
            ).writeBytes(it)
        }
    }

    fun getImage(book: Book, src: String): File {
        return FileUtils.getFile(
            downloadDir,
            cacheFolderName,
            book.getFolderName(),
            cacheImageFolderName,
            "${MD5Utils.md5Encode16(src)}${getImageSuffix(src)}"
        )
    }

    private fun getImageSuffix(src: String): String {
        var suffix = src.substringAfterLast(".").substringBefore(",")
        if (suffix.length > 5) {
            suffix = ".jpg"
        }
        return suffix
    }

    fun getChapterFiles(book: Book): List<String> {
        val fileNameList = arrayListOf<String>()
        if (book.isLocalBook()) {
            return fileNameList
        }
        FileUtils.createFolderIfNotExist(
            downloadDir,
            subDirs = arrayOf(cacheFolderName, book.getFolderName())
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
                cacheFolderName,
                book.getFolderName(),
                formatChapterName(bookChapter)
            )
        }
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {
        if (book.isLocalBook()) {
            return LocalBook.getContext(book, bookChapter)
        } else {
            val file = FileUtils.getFile(
                downloadDir,
                cacheFolderName,
                book.getFolderName(),
                formatChapterName(bookChapter)
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
                cacheFolderName,
                book.getFolderName(),
                formatChapterName(bookChapter)
            ).delete()
        }
    }

    fun formatBookName(name: String): String {
        return name
            .replace(AppPattern.nameRegex, "")
            .trim { it <= ' ' }
    }

    fun formatBookAuthor(author: String): String {
        return author
            .replace(AppPattern.authorRegex, "")
            .trim { it <= ' ' }
    }

    /**
     * 找到相似度最高的章节
     */
    fun getDurChapterIndexByChapterTitle(
        title: String?,
        index: Int,
        chapters: List<BookChapter>,
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
        enableReplace: Boolean,
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