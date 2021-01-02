package io.legado.app.api.controller

import io.legado.app.App
import io.legado.app.api.ReturnData
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.help.ReadBook
import io.legado.app.utils.GSON
import io.legado.app.utils.cnCompare
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getPrefInt
import kotlinx.coroutines.runBlocking

object BookshelfController {

    val bookshelf: ReturnData
        get() {
            val books = App.db.bookDao.all
            val returnData = ReturnData()
            return if (books.isEmpty()) {
                returnData.setErrorMsg("还没有添加小说")
            } else {
                val data = when (App.INSTANCE.getPrefInt(PreferKey.bookshelfSort)) {
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

    fun getChapterList(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.getOrNull(0)
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        val chapterList = App.db.bookChapterDao.getChapterList(bookUrl)
        return returnData.setData(chapterList)
    }

    fun getBookContent(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.getOrNull(0)
        val index = parameters["index"]?.getOrNull(0)?.toInt()
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        if (index == null) {
            return returnData.setErrorMsg("参数index不能为空, 请指定目录序号")
        }
        val book = App.db.bookDao.getBook(bookUrl)
        val chapter = App.db.bookChapterDao.getChapter(bookUrl, index)
        if (book == null || chapter == null) {
            returnData.setErrorMsg("未找到")
        } else {
            val content: String? = BookHelp.getContent(book, chapter)
            if (content != null) {
                saveBookReadIndex(book, index)
                returnData.setData(content)
            } else {
                App.db.bookSourceDao.getBookSource(book.origin)?.let { source ->
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
            App.db.bookDao.insert(book)
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
            App.db.bookChapterDao.getChapter(book.bookUrl, index)?.let {
                book.durChapterTitle = it.title
            }
            App.db.bookDao.update(book)
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
                ReadBook.durChapterIndex = index
            }
        }
    }

}
