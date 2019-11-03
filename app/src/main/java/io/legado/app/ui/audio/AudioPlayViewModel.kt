package io.legado.app.ui.audio

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.model.WebBook
import io.legado.app.ui.book.read.ReadBookViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {
    var inBookshelf = false
    var bookData = MutableLiveData<Book>()
    var chapterSize = 0
    var callBack: ReadBookViewModel.CallBack? = null
    var durChapterIndex = 0
    var durPageIndex = 0
    var isLocalBook = true
    var webBook: WebBook? = null
    private val loadingChapters = arrayListOf<Int>()

    fun initData(intent: Intent) {
        execute {
            inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            val bookUrl = intent.getStringExtra("bookUrl")
            val book = if (!bookUrl.isNullOrEmpty()) {
                App.db.bookDao().getBook(bookUrl)
            } else {
                App.db.bookDao().lastReadBook
            }
            book?.let {
                durChapterIndex = book.durChapterIndex
                durPageIndex = book.durChapterPos
                isLocalBook = book.origin == BookType.local
                bookData.postValue(book)
                App.db.bookSourceDao().getBookSource(book.origin)?.let {
                    webBook = WebBook(it)
                }
                val count = App.db.bookChapterDao().getChapterCount(book.bookUrl)
                if (count == 0) {
                    if (book.tocUrl.isEmpty()) {
                        loadBookInfo(book)
                    } else {
                        loadChapterList(book)
                    }
                } else {
                    if (durChapterIndex > count - 1) {
                        durChapterIndex = count - 1
                    }
                    chapterSize = count
                    callBack?.loadContent()
                }
            }
            saveRead(book)
        }
    }

    private fun loadBookInfo(
        book: Book,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null
    ) {
        execute {
            webBook?.getBookInfo(book, this)
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
            webBook?.getChapterList(book, this)
                ?.onSuccess(Dispatchers.IO) { cList ->
                    if (!cList.isNullOrEmpty()) {
                        if (changeDruChapterIndex == null) {
                            App.db.bookChapterDao().insert(*cList.toTypedArray())
                            chapterSize = cList.size
                            callBack?.loadContent()
                        } else {
                            changeDruChapterIndex(cList)
                        }
                    } else {
                        toast(R.string.error_load_toc)
                    }
                }?.onError {
                    toast(R.string.error_load_toc)
                }
        }
    }

    fun loadContent(book: Book, index: Int) {
        if (addLoading(index)) {
            execute {
                App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                    BookHelp.getContent(book, chapter)?.let {
                        contentLoadFinish(chapter, it)
                        removeLoading(chapter.index)
                    } ?: download(book, chapter)
                } ?: removeLoading(index)
            }.onError {
                removeLoading(index)
            }
        }
    }

    private fun download(book: Book, index: Int) {
        if (addLoading(index)) {
            execute {
                App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                    if (BookHelp.hasContent(book, chapter)) {
                        removeLoading(chapter.index)
                    } else {
                        download(book, chapter)
                    }
                } ?: removeLoading(index)
            }.onError {
                removeLoading(index)
            }
        }
    }

    private fun download(book: Book, chapter: BookChapter) {
        webBook?.getContent(book, chapter, scope = this)
            ?.onSuccess(Dispatchers.IO) { content ->
                if (content.isNullOrEmpty()) {
                    contentLoadFinish(chapter, context.getString(R.string.content_empty))
                    removeLoading(chapter.index)
                } else {
                    BookHelp.saveContent(book, chapter, content)
                    contentLoadFinish(chapter, content)
                    removeLoading(chapter.index)
                }
            }?.onError {
                contentLoadFinish(chapter, it.localizedMessage)
                removeLoading(chapter.index)
            }
    }

    private fun addLoading(index: Int): Boolean {
        synchronized(this) {
            if (loadingChapters.contains(index)) return false
            loadingChapters.add(index)
            return true
        }
    }

    private fun removeLoading(index: Int) {
        synchronized(this) {
            loadingChapters.remove(index)
        }
    }

    private fun contentLoadFinish(chapter: BookChapter, content: String) {
        if (chapter.index == durChapterIndex) {
            callBack?.contentLoadFinish(chapter, content)
        }
    }

    fun changeTo(book: Book) {
        execute {
            bookData.value?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
            withContext(Dispatchers.Main) {
                callBack?.upContent()
            }
            App.db.bookDao().insert(book)
            bookData.postValue(book)
            App.db.bookSourceDao().getBookSource(book.origin)?.let {
                webBook = WebBook(it)
            }
            if (book.tocUrl.isEmpty()) {
                loadBookInfo(book) { upChangeDurChapterIndex(book, it) }
            } else {
                loadChapterList(book) { upChangeDurChapterIndex(book, it) }
            }
        }
    }

    private fun upChangeDurChapterIndex(book: Book, chapters: List<BookChapter>) {
        execute {
            durChapterIndex = BookHelp.getDurChapterIndexByChapterTitle(
                book.durChapterTitle,
                book.durChapterIndex,
                chapters
            )
            book.durChapterIndex = durChapterIndex
            book.durChapterTitle = chapters[durChapterIndex].title
            App.db.bookDao().update(book)
            App.db.bookChapterDao().insert(*chapters.toTypedArray())
            chapterSize = chapters.size
            callBack?.loadContent()
        }
    }

    fun saveRead(book: Book? = bookData.value) {
        execute {
            book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = durPageIndex
                App.db.bookDao().update(book)
            }
        }
    }

    interface CallBack {
        fun loadContent()
        fun contentLoadFinish(bookChapter: BookChapter, content: String)
        fun upContent()
    }
}