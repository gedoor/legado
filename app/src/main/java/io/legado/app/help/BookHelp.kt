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
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ricecode.similarity.JaroWinklerStrategy
import net.ricecode.similarity.StringSimilarityServiceImpl
import org.jetbrains.anko.toast
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object BookHelp {
    private const val cacheFolderName = "book_cache"
    private const val cacheImageFolderName = "images"
    private val downloadDir: File = App.INSTANCE.externalFilesDir
    private val downloadImages = CopyOnWriteArraySet<String>()

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
            val file = FileUtils.getFile(downloadDir, cacheFolderName)
            file.listFiles()?.forEach { bookFile ->
                if (!bookFolderNames.contains(bookFile.name)) {
                    FileUtils.deleteFile(bookFile.absolutePath)
                }
            }
        }
    }

    suspend fun saveContent(book: Book, bookChapter: BookChapter, content: String) {
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

    suspend fun saveImage(book: Book, src: String) {
        while (downloadImages.contains(src)) {
            delay(100)
        }
        if (getImage(book, src).exists()) {
            return
        }
        downloadImages.add(src)
        val analyzeUrl = AnalyzeUrl(src)
        try {
            analyzeUrl.getResponseBytes(book.origin)?.let {
                FileUtils.createFileIfNotExist(
                    downloadDir,
                    cacheFolderName,
                    book.getFolderName(),
                    cacheImageFolderName,
                    "${MD5Utils.md5Encode16(src)}${getImageSuffix(src)}"
                ).writeBytes(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            downloadImages.remove(src)
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

    // 检测该章节是否下载
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
     * 根据目录名获取当前章节
     */
    fun getDurChapter(
        oldDurChapterIndex: Int,
        oldChapterListSize: Int,
        oldDurChapterName: String?,
        newChapterList: List<BookChapter>
    ): Int {
        if (oldChapterListSize == 0) return 0
        val oldChapterNum = getChapterNum(oldDurChapterName)
        val oldName = getPureChapterName(oldDurChapterName)
        val newChapterSize = newChapterList.size
        val min = max(
            0,
            min(
                oldDurChapterIndex,
                oldDurChapterIndex - oldChapterListSize + newChapterSize
            ) - 10
        )
        val max = min(
            newChapterSize - 1,
            max(
                oldDurChapterIndex,
                oldDurChapterIndex - oldChapterListSize + newChapterSize
            ) + 10
        )
        var nameSim = 0.0
        var newIndex = 0
        var newNum = 0
        if (oldName.isNotEmpty()) {
            val service = StringSimilarityServiceImpl(JaroWinklerStrategy())
            for (i in min..max) {
                val newName = getPureChapterName(newChapterList[i].title)
                val temp = service.score(oldName, newName)
                if (temp > nameSim) {
                    nameSim = temp
                    newIndex = i
                }
            }
        }
        if (nameSim < 0.96 && oldChapterNum > 0) {
            for (i in min..max) {
                val temp = getChapterNum(newChapterList[i].title)
                if (temp == oldChapterNum) {
                    newNum = temp
                    newIndex = i
                    break
                } else if (abs(temp - oldChapterNum) < abs(newNum - oldChapterNum)) {
                    newNum = temp
                    newIndex = i
                }
            }
        }
        return if (nameSim > 0.96 || abs(newNum - oldChapterNum) < 1) {
            newIndex
        } else {
            min(max(0, newChapterList.size - 1), oldDurChapterIndex)
        }
    }

    private val chapterNamePattern by lazy {
        Pattern.compile("^(.*?第([\\d零〇一二两三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟０-９\\s]+)[章节篇回集])[、，。　：:.\\s]*")
    }

    private fun getChapterNum(chapterName: String?): Int {
        if (chapterName != null) {
            val matcher: Matcher = chapterNamePattern.matcher(chapterName)
            if (matcher.find()) {
                return StringUtils.stringToInt(matcher.group(2))
            }
        }
        return -1
    }

    @Suppress("SpellCheckingInspection")
    private val regexOther by lazy {
        // 所有非字母数字中日韩文字 CJK区+扩展A-F区
        return@lazy "[^\\w\\u4E00-\\u9FEF〇\\u3400-\\u4DBF\\u20000-\\u2A6DF\\u2A700-\\u2EBEF]".toRegex()
    }

    private val regexA by lazy {
        return@lazy "\\s".toRegex()
    }

    private val regexB by lazy {
        return@lazy "^第.*?章|[(\\[][^()\\[\\]]{2,}[)\\]]$".toRegex()
    }

    private fun getPureChapterName(chapterName: String?): String {
        return if (chapterName == null) "" else StringUtils.fullToHalf(chapterName)
            .replace(regexA, "")
            .replace(regexB, "")
            .replace(regexOther, "")
    }

    private var bookName: String? = null
    private var bookOrigin: String? = null
    private var replaceRules: List<ReplaceRule> = arrayListOf()

    @Synchronized
    fun upReplaceRules() {
        val o = bookOrigin
        bookName?.let {
            replaceRules = if (o.isNullOrEmpty()) {
                App.db.replaceRuleDao().findEnabledByScope(it)
            } else {
                App.db.replaceRuleDao().findEnabledByScope(it, o)
            }
        }
    }

    suspend fun disposeContent(
        book: Book,
        title: String,
        content: String,
    ): List<String> {
        var title1 = title
        var content1 = content
        if (book.getUseReplaceRule()) {
            synchronized(this) {
                if (bookName != book.name || bookOrigin != book.origin) {
                    bookName = book.name
                    bookOrigin = book.origin
                    replaceRules = if (bookOrigin.isNullOrEmpty()) {
                        App.db.replaceRuleDao().findEnabledByScope(bookName!!)
                    } else {
                        App.db.replaceRuleDao().findEnabledByScope(bookName!!, bookOrigin!!)
                    }
                }
            }
            replaceRules.forEach { item ->
                item.pattern.let {
                    if (it.isNotEmpty()) {
                        try {
                            content1 = if (item.isRegex) {
                                content1.replace(it.toRegex(), item.replacement)
                            } else {
                                content1.replace(it, item.replacement)
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
        if (book.getReSegment()) {
            content1 = ContentHelp.reSegment(content1, title1)
        }
        try {
            when (AppConfig.chineseConverterType) {
                1 -> {
                    title1 = HanLP.convertToSimplifiedChinese(title1)
                    content1 = HanLP.convertToSimplifiedChinese(content1)
                }
                2 -> {
                    title1 = HanLP.convertToTraditionalChinese(title1)
                    content1 = HanLP.convertToTraditionalChinese(content1)
                }
            }
        } catch (e: Exception) {
            withContext(Main) {
                App.INSTANCE.toast("简繁转换出错")
            }
        }
        val contents = arrayListOf<String>()
        content1.split("\n").forEach {
            val str = it.replace("^[\\n\\s\\r]+".toRegex(), "")
            if (contents.isEmpty()) {
                contents.add(title1)
                if (str != title1 && str.isNotEmpty()) {
                    contents.add("${ReadBookConfig.paragraphIndent}$str")
                }
            } else if (str.isNotEmpty()) {
                contents.add("${ReadBookConfig.paragraphIndent}$str")
            }
        }
        return contents
    }
}