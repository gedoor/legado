package io.legado.app.api.controller

import android.util.Base64
import androidx.core.graphics.drawable.toBitmap
import io.legado.app.R
import io.legado.app.api.ReturnData
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.help.CacheManager
import io.legado.app.help.ContentProcessor
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.storage.AppWebDav
import io.legado.app.model.BookCover
import io.legado.app.model.ReadBook
import io.legado.app.model.localBook.EpubFile
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.localBook.UmdFile
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.*
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import timber.log.Timber

object BookController {

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
                    WebBook.getChapterListAwait(this, bookSource, book)
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
        val chapter = appDb.bookChapterDao.getChapter(bookUrl, index)
        if (book == null || chapter == null) {
            return returnData.setErrorMsg("未找到")
        }
        var content: String? = BookHelp.getContent(book, chapter)
        if (content != null) {
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            saveBookReadIndex(book, index)
            return returnData.setData(
                contentProcessor.getContent(book, chapter, content, includeTitle = false)
                    .joinToString("\n")
            )
        }
        val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            ?: return returnData.setErrorMsg("未找到书源")
        try {
            content = runBlocking {
                WebBook.getContentAwait(this, bookSource, book, chapter)
            }
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            saveBookReadIndex(book, index)
            returnData.setData(
                contentProcessor.getContent(book, chapter, content, includeTitle = false)
                    .joinToString("\n")
            )
        } catch (e: Exception) {
            returnData.setErrorMsg(e.msg)
        }
        return returnData
    }

    /**
     * 保存书籍
     */
    fun saveBook(postData: String?): ReturnData {
        val book = GSON.fromJsonObject<Book>(postData)
        val returnData = ReturnData()
        if (book != null) {
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
        try {
            val fileName = parameters["fileName"]?.firstOrNull()
                ?: return returnData.setErrorMsg("fileName 不能为空")
            val fileData = parameters["fileData"]?.firstOrNull()
                ?: return returnData.setErrorMsg("fileData 不能为空")
            val file = FileUtils.createFileIfNotExist(LocalBook.cacheFolder, fileName)
            val fileBytes = Base64.decode(fileData.substringAfter("base64,"), Base64.DEFAULT)
            file.writeBytes(fileBytes)
            val nameAuthor = LocalBook.analyzeNameAuthor(fileName)
            val book = Book(
                bookUrl = file.absolutePath,
                name = nameAuthor.first,
                author = nameAuthor.second,
                originName = fileName,
                coverUrl = FileUtils.getPath(
                    appCtx.externalFiles,
                    "covers",
                    "${MD5Utils.md5Encode16(file.absolutePath)}.jpg"
                )
            )
            if (book.isEpub()) EpubFile.upBookInfo(book)
            if (book.isUmd()) UmdFile.upBookInfo(book)
            appDb.bookDao.insert(book)
        } catch (e: Exception) {
            Timber.e(e)
            return returnData.setErrorMsg(
                e.localizedMessage ?: appCtx.getString(R.string.unknown_error)
            )
        }
        return returnData.setData(true)
    }

    fun saveWebReadConfig(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData?.let {
            CacheManager.put("webReadConfig", postData)
        }
        return returnData.setData("")
    }

    fun getWebReadConfig(): ReturnData {
        val returnData = ReturnData()
        val data = CacheManager.get("webReadConfig") ?: "{}"
        return returnData.setData(data)
    }

}
