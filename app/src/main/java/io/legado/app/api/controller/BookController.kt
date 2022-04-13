package io.legado.app.api.controller

import android.net.Uri
import android.util.Base64
import androidx.core.graphics.drawable.toBitmap
import androidx.documentfile.provider.DocumentFile
import io.legado.app.api.ReturnData
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.help.CacheManager
import io.legado.app.help.ContentProcessor
import io.legado.app.help.config.AppConfig
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.storage.AppWebDav
import io.legado.app.model.BookCover
import io.legado.app.model.ReadBook
import io.legado.app.model.localBook.EpubFile
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.localBook.UmdFile
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.*
import io.legado.app.ui.book.read.page.provider.ImageProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

object BookController {

    private lateinit var book: Book
    private var bookSource: BookSource? = null
    private var bookUrl: String = ""

    /**
     * 书架所有书籍
     */
    val bookshelf: ReturnData
        get() {
            val books = appDb.bookDao.all
            val returnData = ReturnData()
            return if (books.isEmpty()) {
                returnData.setErrorMsg("还没有添加小说")
            } else {
                val data = when (appCtx.getPrefInt(PreferKey.bookshelfSort)) {
                    1 -> books.sortedByDescending { it.latestChapterTime }
                    2 -> books.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> books.sortedBy { it.order }
                    else -> books.sortedByDescending { it.durChapterTime }
                }
                returnData.setData(data)
            }
        }

    /**
     * 获取封面
     */
    fun getCover(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val coverPath = parameters["path"]?.firstOrNull()
        val ftBitmap = ImageLoader.loadBitmap(appCtx, coverPath).submit()
        return try {
            returnData.setData(ftBitmap.get())
        } catch (e: Exception) {
            returnData.setData(BookCover.defaultDrawable.toBitmap())
        }
    }

    /**
     * 获取正文图片
     */
    fun getImg(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val bookUrl = parameters["url"]?.firstOrNull()
            ?: return returnData.setErrorMsg("bookUrl为空")
        val src = parameters["path"]?.firstOrNull()
            ?: return returnData.setErrorMsg("图片链接为空")
        val width = parameters["width"]?.firstOrNull()?.toInt() ?: 640
        if (this.bookUrl != bookUrl) {
            this.book = appDb.bookDao.getBook(bookUrl)
                ?: return returnData.setErrorMsg("bookUrl不对")
            this.bookSource = appDb.bookSourceDao.getBookSource(book.origin)
        }
        this.bookUrl = bookUrl
        return ImageProvider.getImage(book, src, bookSource, width, width)?.let {
            returnData.setData(it)
        } ?: returnData.setErrorMsg("图片加载失败或不存在")
    }

    /**
     * 更新目录
     */
    fun refreshToc(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        try {
            val bookUrl = parameters["url"]?.firstOrNull()
            if (bookUrl.isNullOrEmpty()) {
                return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
            }
            val book = appDb.bookDao.getBook(bookUrl)
                ?: return returnData.setErrorMsg("bookUrl不对")
            if (book.isLocalBook()) {
                val toc = LocalBook.getChapterList(book)
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                appDb.bookDao.update(book)
                return returnData.setData(toc)
            } else {
                val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                    ?: return returnData.setErrorMsg("未找到对应书源,请换源")
                val toc = runBlocking {
                    if (book.tocUrl.isBlank()) {
                        WebBook.getBookInfoAwait(this, bookSource, book)
                    }
                    WebBook.getChapterListAwait(this, bookSource, book).getOrThrow()
                }
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                appDb.bookDao.update(book)
                return returnData.setData(toc)
            }
        } catch (e: Exception) {
            return returnData.setErrorMsg(e.localizedMessage ?: "refresh toc error")
        }
    }

    /**
     * 获取目录
     */
    fun getChapterList(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.firstOrNull()
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        val chapterList = appDb.bookChapterDao.getChapterList(bookUrl)
        if (chapterList.isEmpty()) {
            return refreshToc(parameters)
        }
        return returnData.setData(chapterList)
    }

    /**
     * 获取正文
     */
    fun getBookContent(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.firstOrNull()
        val index = parameters["index"]?.firstOrNull()?.toInt()
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        if (index == null) {
            return returnData.setErrorMsg("参数index不能为空, 请指定目录序号")
        }
        val book = appDb.bookDao.getBook(bookUrl)
        val chapter = runBlocking {
            var chapter = appDb.bookChapterDao.getChapter(bookUrl, index)
            var wait = 0
            while (chapter == null && wait < 30) {
                delay(1000)
                chapter = appDb.bookChapterDao.getChapter(bookUrl, index)
                wait++
            }
            chapter
        }
        if (book == null || chapter == null) {
            return returnData.setErrorMsg("未找到")
        }
        var content: String? = BookHelp.getContent(book, chapter)
        if (content != null) {
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            saveBookReadIndex(book, index)
            content = runBlocking {
                contentProcessor.getContent(book, chapter, content!!, includeTitle = false)
                    .joinToString("\n")
            }
            return returnData.setData(content)
        }
        val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            ?: return returnData.setErrorMsg("未找到书源")
        try {
            content = runBlocking {
                WebBook.getContentAwait(this, bookSource, book, chapter).let {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    saveBookReadIndex(book, index)
                    contentProcessor.getContent(book, chapter, it, includeTitle = false)
                        .joinToString("\n")
                }
            }
            returnData.setData(content)
        } catch (e: Exception) {
            returnData.setErrorMsg(e.msg)
        }
        return returnData
    }

    /**
     * 保存书籍
     */
    fun saveBook(postData: String?): ReturnData {
        val returnData = ReturnData()
        GSON.fromJsonObject<Book>(postData).getOrNull()?.let { book ->
            book.save()
            AppWebDav.uploadBookProgress(book)
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
                ReadBook.durChapterIndex = book.durChapterIndex
            }
            return returnData.setData("")
        }
        return returnData.setErrorMsg("格式不对")
    }

    /**
     * 保存进度
     */
    private fun saveBookReadIndex(book: Book, index: Int) {
        book.durChapterIndex = index
        book.durChapterTime = System.currentTimeMillis()
        appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let {
            book.durChapterTitle = it.title
        }
        appDb.bookDao.update(book)
        AppWebDav.uploadBookProgress(book)
        if (ReadBook.book?.bookUrl == book.bookUrl) {
            ReadBook.book = book
            ReadBook.durChapterIndex = index
            ReadBook.clearTextChapter()
            ReadBook.loadContent(true)
        }
    }

    /**
     * 添加本地书籍
     */
    fun addLocalBook(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val fileName = parameters["fileName"]?.firstOrNull()
            ?: return returnData.setErrorMsg("fileName 不能为空")
        val fileData = parameters["fileData"]?.firstOrNull()
            ?: return returnData.setErrorMsg("fileData 不能为空")
        kotlin.runCatching {
            val defaultBookTreeUri = AppConfig.defaultBookTreeUri
            if (defaultBookTreeUri.isNullOrBlank()) return returnData.setErrorMsg("没有设置书籍保存位置!")
            val treeUri = Uri.parse(defaultBookTreeUri)
            val fileBytes =
                Base64.decode(fileData.substringAfter("base64,"), Base64.DEFAULT)
            val uri = if (treeUri.isContentScheme()) {
                val treeDoc = DocumentFile.fromTreeUri(appCtx, treeUri)
                var doc = treeDoc!!.findFile(fileName)
                if (doc == null) {
                    doc = treeDoc.createFile(FileUtils.getMimeType(fileName), fileName)
                        ?: throw SecurityException("Permission Denial")
                }
                appCtx.contentResolver.openOutputStream(doc.uri)!!.use { oStream ->
                    oStream.write(fileBytes)
                }
                doc.uri
            } else {
                val treeFile = File(treeUri.path!!)
                val file = treeFile.getFile(fileName)
                FileOutputStream(file).use { oStream ->
                    oStream.write(fileBytes)
                }
                Uri.fromFile(file)
            }
            val nameAuthor = LocalBook.analyzeNameAuthor(fileName)
            val book = Book(
                bookUrl = uri.toString(),
                name = nameAuthor.first,
                author = nameAuthor.second,
                originName = fileName,
                coverUrl = FileUtils.getPath(
                    appCtx.externalFiles,
                    "covers",
                    "${MD5Utils.md5Encode16(uri.toString())}.jpg"
                )
            )
            if (book.isEpub()) EpubFile.upBookInfo(book)
            if (book.isUmd()) UmdFile.upBookInfo(book)
            appDb.bookDao.insert(book)
        }.onFailure {
            return when (it) {
                is SecurityException -> returnData.setErrorMsg("需重新设置书籍保存位置!")
                else -> returnData.setErrorMsg("保存书籍错误\n${it.localizedMessage}")
            }
        }
        return returnData.setData(true)
    }

    /**
     * 保存web阅读界面配置
     */
    fun saveWebReadConfig(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData?.let {
            CacheManager.put("webReadConfig", postData)
        } ?: CacheManager.delete("webReadConfig")
        return returnData.setData("")
    }

    /**
     * 获取web阅读界面配置
     */
    fun getWebReadConfig(): ReturnData {
        val returnData = ReturnData()
        val data = CacheManager.get("webReadConfig")
            ?: return returnData.setErrorMsg("没有配置")
        return returnData.setData(data)
    }

}
