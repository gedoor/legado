package io.legado.app.help

import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.*
import kotlinx.coroutines.delay
import org.apache.commons.text.similarity.JaccardSimilarity
import splitties.init.appCtx
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
    private val downloadDir: File = appCtx.externalFilesDir
    private val downloadImages = CopyOnWriteArraySet<String>()

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
            appDb.bookDao.all.forEach {
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
            bookChapter.getFileName(),
        ).writeText(content)
        //保存图片
        content.split("\n").forEach {
            val matcher = AppPattern.imgPattern.matcher(it)
            if (matcher.find()) {
                matcher.group(1)?.let { src ->
                    val mSrc = NetworkUtils.getAbsoluteURL(bookChapter.url, src)
                    saveImage(book, mSrc)
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
            analyzeUrl.getByteArray(book.origin).let {
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
        if (book.isLocalTxt()) {
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
        return if (book.isLocalTxt()) {
            true
        } else {
            FileUtils.exists(
                downloadDir,
                cacheFolderName,
                book.getFolderName(),
                bookChapter.getFileName()
            )
        }
    }

    fun hasImageContent(book: Book, bookChapter: BookChapter): Boolean {
        if (!hasContent(book, bookChapter)) {
            return false
        }
        getContent(book, bookChapter)?.let {
            val matcher = AppPattern.imgPattern.matcher(it)
            while (matcher.find()) {
                matcher.group(1)?.let { src ->
                    val image = getImage(book, src)
                    if (!image.exists()) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun getContent(book: Book, bookChapter: BookChapter): String? {
        if (book.isLocalTxt()) {
            return LocalBook.getContext(book, bookChapter)
        } else if (book.isEpub() && !hasContent(book, bookChapter)) {
            val string = LocalBook.getContext(book, bookChapter)
            string?.let {
                FileUtils.createFileIfNotExist(
                    downloadDir,
                    cacheFolderName,
                    book.getFolderName(),
                    bookChapter.getFileName(),
                ).writeText(it)
            }
            return string
        } else {
            val file = FileUtils.getFile(
                downloadDir,
                cacheFolderName,
                book.getFolderName(),
                bookChapter.getFileName()
            )
            if (file.exists()) {
                return file.readText()
            }
        }
        return null
    }

    fun reverseContent(book: Book, bookChapter: BookChapter) {
        if (!book.isLocalBook()) {
            val file = FileUtils.getFile(
                downloadDir,
                cacheFolderName,
                book.getFolderName(),
                bookChapter.getFileName()
            )
            if (file.exists()) {
                val text = file.readText()
                val stringBuilder = StringBuilder()
                text.toStringArray().forEach {
                    stringBuilder.insert(0, it)
                }
                file.writeText(stringBuilder.toString())
            }
        }
    }

    fun delContent(book: Book, bookChapter: BookChapter) {
        if (book.isLocalTxt()) {
            return
        } else {
            FileUtils.createFileIfNotExist(
                downloadDir,
                cacheFolderName,
                book.getFolderName(),
                bookChapter.getFileName()
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

    private val jaccardSimilarity by lazy {
        JaccardSimilarity()
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
        if (oldChapterListSize == 0) return oldDurChapterIndex
        if (newChapterList.isEmpty()) return oldDurChapterIndex
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
            for (i in min..max) {
                val newName = getPureChapterName(newChapterList[i].title)
                val temp = jaccardSimilarity.apply(oldName, newName)
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

}