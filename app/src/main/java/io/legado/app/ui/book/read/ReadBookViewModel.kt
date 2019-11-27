package io.legado.app.ui.book.read

import android.app.Application
import android.content.Intent
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.IntentDataHelp
import io.legado.app.model.WebBook
import io.legado.app.service.help.ReadAloud
import io.legado.app.service.help.ReadBook
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadBookViewModel(application: Application) : BaseViewModel(application) {

    fun initData(intent: Intent) {
        execute {
            ReadBook.inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            IntentDataHelp.getData<Book>(intent.getStringExtra("key"))?.let {
                initBook(it)
            } ?: intent.getStringExtra("bookUrl")?.let {
                App.db.bookDao().getBook(it)?.let { book ->
                    initBook(book)
                }
            } ?: App.db.bookDao().lastReadBook?.let {
                initBook(it)
            }
        }
    }

    private fun initBook(book: Book) {
        ReadBook.book = book
        ReadBook.titleDate.postValue(book.name)
        ReadBook.durChapterIndex = book.durChapterIndex
        ReadBook.durPageIndex = book.durChapterPos
        ReadBook.isLocalBook = book.origin == BookType.local
        App.db.bookSourceDao().getBookSource(book.origin)?.let {
            ReadBook.webBook = WebBook(it)
        }
        val count = App.db.bookChapterDao().getChapterCount(book.bookUrl)
        if (count == 0) {
            if (book.tocUrl.isEmpty()) {
                loadBookInfo(book)
            } else {
                loadChapterList(book)
            }
        } else {
            if (ReadBook.durChapterIndex > count - 1) {
                ReadBook.durChapterIndex = count - 1
            }
            ReadBook.chapterSize = count
            ReadBook.callBack?.loadContent()
        }
        if (ReadBook.inBookshelf) {
            saveRead()
        }
    }

    private fun loadBookInfo(
        book: Book,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null
    ) {
        execute {
            ReadBook.webBook?.getBookInfo(book, this)
                ?.onSuccess {
                    loadChapterList(book, changeDruChapterIndex)
                }
        }
    }

    private fun loadChapterList(
        book: Book,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null
    ) {
        execute {
            ReadBook.webBook?.getChapterList(book, this)
                ?.onSuccess(IO) { cList ->
                    if (!cList.isNullOrEmpty()) {
                        if (changeDruChapterIndex == null) {
                            App.db.bookChapterDao().insert(*cList.toTypedArray())
                            ReadBook.chapterSize = cList.size
                            ReadBook.callBack?.loadContent()
                        } else {
                            changeDruChapterIndex(cList)
                        }
                    } else {
                        toast(R.string.error_load_toc)
                    }
                }?.onError {
                    toast(R.string.error_load_toc)
                } ?: autoChangeSource()
        }
    }

    fun moveToNextChapter(upContent: Boolean) {
        ReadBook.durChapterIndex++
        ReadBook.prevTextChapter = ReadBook.curTextChapter
        ReadBook.curTextChapter = ReadBook.nextTextChapter
        ReadBook.nextTextChapter = null
        ReadBook.book?.let {
            if (ReadBook.curTextChapter == null) {
                loadContent(ReadBook.durChapterIndex)
            } else if (upContent) {
                ReadBook.callBack?.upContent()
            }
            loadContent(ReadBook.durChapterIndex.plus(1))
            launch(IO) {
                for (i in 2..10) {
                    delay(100)
                    download(ReadBook.durChapterIndex + i)
                }
            }
        }
    }

    fun moveToPrevChapter(upContent: Boolean) {
        ReadBook.durChapterIndex--
        ReadBook.nextTextChapter = ReadBook.curTextChapter
        ReadBook.curTextChapter = ReadBook.prevTextChapter
        ReadBook.prevTextChapter = null
        ReadBook.book?.let {
            if (ReadBook.curTextChapter == null) {
                loadContent(ReadBook.durChapterIndex)
            } else if (upContent) {
                ReadBook.callBack?.upContent()
            }
            loadContent(ReadBook.durChapterIndex.minus(1))
            launch(IO) {
                for (i in -5..-2) {
                    delay(100)
                    download(ReadBook.durChapterIndex + i)
                }
            }
        }
    }

    fun loadContent(index: Int) {
        ReadBook.book?.let { book ->
            if (addLoading(index)) {
                execute {
                    App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                        BookHelp.getContent(book, chapter)?.let {
                            contentLoadFinish(chapter, it)
                            removeLoading(chapter.index)
                        } ?: download(chapter)
                    } ?: removeLoading(index)
                }.onError {
                    removeLoading(index)
                }
            }
        }
    }

    private fun download(index: Int) {
        ReadBook.book?.let { book ->
            if (addLoading(index)) {
                execute {
                    App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                        if (BookHelp.hasContent(book, chapter)) {
                            removeLoading(chapter.index)
                        } else {
                            download(chapter)
                        }
                    } ?: removeLoading(index)
                }.onError {
                    removeLoading(index)
                }
            }
        }
    }

    private fun download(chapter: BookChapter) {
        ReadBook.book?.let { book ->
            ReadBook.webBook?.getContent(book, chapter, scope = this)
                ?.onSuccess(IO) { content ->
                    if (content.isNullOrEmpty()) {
                        contentLoadFinish(chapter, context.getString(R.string.content_empty))
                        removeLoading(chapter.index)
                    } else {
                        BookHelp.saveContent(book, chapter, content)
                        contentLoadFinish(chapter, content)
                        removeLoading(chapter.index)
                    }
                }?.onError {
                    contentLoadFinish(chapter, it.localizedMessage ?: "未知错误")
                    removeLoading(chapter.index)
                }
        }
    }

    private fun addLoading(index: Int): Boolean {
        synchronized(this) {
            if (ReadBook.loadingChapters.contains(index)) return false
            ReadBook.loadingChapters.add(index)
            return true
        }
    }

    private fun removeLoading(index: Int) {
        synchronized(this) {
            ReadBook.loadingChapters.remove(index)
        }
    }

    private fun contentLoadFinish(chapter: BookChapter, content: String) {
        execute {
            if (chapter.index in ReadBook.durChapterIndex - 1..ReadBook.durChapterIndex + 1) {
                val c = BookHelp.disposeContent(
                    chapter.title,
                    ReadBook.book!!.name,
                    ReadBook.webBook?.bookSource?.bookSourceUrl,
                    content,
                    ReadBook.book!!.useReplaceRule
                )
                ReadBook.callBack?.contentLoadFinish(chapter, c)
            }
        }
    }

    fun changeTo(book1: Book) {
        execute {
            ReadBook.book?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
            ReadBook.prevTextChapter = null
            ReadBook.curTextChapter = null
            ReadBook.nextTextChapter = null
            withContext(Main) {
                ReadBook.callBack?.upContent()
            }
            App.db.bookDao().insert(book1)
            ReadBook.book = book1
            App.db.bookSourceDao().getBookSource(book1.origin)?.let {
                ReadBook.webBook = WebBook(it)
            }
            if (book1.tocUrl.isEmpty()) {
                loadBookInfo(book1) { upChangeDurChapterIndex(book1, it) }
            } else {
                loadChapterList(book1) { upChangeDurChapterIndex(book1, it) }
            }
        }
    }

    private fun autoChangeSource() {

    }

    private fun upChangeDurChapterIndex(book: Book, chapters: List<BookChapter>) {
        execute {
            ReadBook.durChapterIndex = BookHelp.getDurChapterIndexByChapterTitle(
                book.durChapterTitle,
                book.durChapterIndex,
                chapters
            )
            book.durChapterIndex = ReadBook.durChapterIndex
            book.durChapterTitle = chapters[ReadBook.durChapterIndex].title
            App.db.bookDao().update(book)
            App.db.bookChapterDao().insert(*chapters.toTypedArray())
            ReadBook.chapterSize = chapters.size
            ReadBook.callBack?.loadContent()
        }
    }

    fun openChapter(index: Int) {
        ReadBook.prevTextChapter = null
        ReadBook.curTextChapter = null
        ReadBook.nextTextChapter = null
        ReadBook.callBack?.upContent()
        if (index != ReadBook.durChapterIndex) {
            ReadBook.durChapterIndex = index
            ReadBook.durPageIndex = 0
        }
        saveRead()
        ReadBook.callBack?.loadContent()
    }

    fun saveRead() {
        execute {
            ReadBook.book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = ReadBook.durChapterIndex
                book.durChapterPos = ReadBook.durPageIndex
                ReadBook.curTextChapter?.let {
                    book.durChapterTitle = it.title
                }
                App.db.bookDao().update(book)
            }
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            ReadBook.book?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun upBookSource() {
        execute {
            ReadBook.book?.let { book ->
                App.db.bookSourceDao().getBookSource(book.origin)?.let {
                    ReadBook.webBook = WebBook(it)
                }
            }
        }
    }

    fun refreshContent(book: Book) {
        execute {
            App.db.bookChapterDao().getChapter(book.bookUrl, ReadBook.durChapterIndex)
                ?.let { chapter ->
                BookHelp.delContent(book, chapter)
                    loadContent(ReadBook.durChapterIndex)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ReadAloud.stop(context)
    }

    interface CallBack {
        fun loadContent()
        fun contentLoadFinish(bookChapter: BookChapter, content: String)
        fun upContent()
    }
}