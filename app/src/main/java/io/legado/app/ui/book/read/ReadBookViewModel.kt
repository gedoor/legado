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
            ReadBook.saveRead()
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

    fun moveToPrevChapter(upContent: Boolean) {
        ReadBook.durChapterIndex--
        ReadBook.nextTextChapter = ReadBook.curTextChapter
        ReadBook.curTextChapter = ReadBook.prevTextChapter
        ReadBook.prevTextChapter = null
        ReadBook.book?.let {
            if (ReadBook.curTextChapter == null) {
                ReadBook.loadContent(ReadBook.durChapterIndex)
            } else if (upContent) {
                ReadBook.callBack?.upContent()
            }
            ReadBook.loadContent(ReadBook.durChapterIndex.minus(1))
            launch(IO) {
                for (i in -5..-2) {
                    delay(100)
                    ReadBook.download(ReadBook.durChapterIndex + i)
                }
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
        ReadBook.saveRead()
        ReadBook.callBack?.loadContent()
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
                    ReadBook.loadContent(ReadBook.durChapterIndex)
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
        fun curChapterChanged()
    }
}