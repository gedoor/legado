package io.legado.app.api.controller

import io.legado.app.R
import io.legado.app.api.ReturnData
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.help.ImageLoader
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.utils.GSON
import io.legado.app.utils.cnCompare
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getPrefInt
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx

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

    fun getCover(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val coverPath = parameters["path"]?.firstOrNull()
        val ftBitmap = ImageLoader.loadBitmap(appCtx, coverPath)
            .placeholder(CoverImageView.defaultDrawable)
            .error(CoverImageView.defaultDrawable)
            .submit()
        return returnData.setData(ftBitmap.get())
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
                return if (toc.isEmpty()) {
                    returnData.setErrorMsg(appCtx.getString(R.string.error_load_toc))
                } else {
                    returnData.setData(toc)
                }
            } else {
                val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                    ?: return returnData.setErrorMsg("未找到对应书源,请换源")
                val webBook = WebBook(bookSource)
                val toc = runBlocking {
                    if (book.tocUrl.isBlank()) {
                        webBook.getBookInfoAwait(this, book)
                    }
                    webBook.getChapterListAwait(this, book)
                }
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                appDb.bookDao.update(book)
                return if (toc.isEmpty()) {
                    returnData.setErrorMsg(appCtx.getString(R.string.error_load_toc))
                } else {
                    returnData.setData(toc)
                }
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
            returnData.setErrorMsg("未找到")
        } else {
            val content: String? = BookHelp.getContent(book, chapter)
            if (content != null) {
                saveBookReadIndex(book, index)
                returnData.setData(content)
            } else {
                appDb.bookSourceDao.getBookSource(book.origin)?.let { source ->
                    runBlocking {
                        WebBook(source).getContentAwait(this, book, chapter)
                    }.let {
                        saveBookReadIndex(book, index)
                        returnData.setData(it)
                    }
                } ?: returnData.setErrorMsg("未找到书源")
            }
        }
        return returnData
    }

    fun saveBook(postData: String?): ReturnData {
        val book = GSON.fromJsonObject<Book>(postData)
        val returnData = ReturnData()
        if (book != null) {
            book.save()
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
                ReadBook.durChapterIndex = book.durChapterIndex
            }
            return returnData.setData("")
        }
        return returnData.setErrorMsg("格式不对")
    }

    private fun saveBookReadIndex(book: Book, index: Int) {
        if (index > book.durChapterIndex) {
            book.durChapterIndex = index
            book.durChapterTime = System.currentTimeMillis()
            appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let {
                book.durChapterTitle = it.title
            }
            appDb.bookDao.update(book)
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
                ReadBook.durChapterIndex = index
            }
        }
    }

}
