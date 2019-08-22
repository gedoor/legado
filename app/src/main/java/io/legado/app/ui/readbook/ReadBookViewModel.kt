package io.legado.app.ui.readbook

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.WebBook
import io.legado.app.service.ReadAloudService
import io.legado.app.ui.widget.page.TextChapter
import kotlinx.coroutines.Dispatchers.IO

class ReadBookViewModel(application: Application) : BaseViewModel(application) {
    var inBookshelf = false
    var bookData = MutableLiveData<Book>()
    var chapterSize = 0
    var bookSource: BookSource? = null
    var webBook: WebBook? = null
    var callBack: CallBack? = null
    var durChapterIndex = 0
    var durPageIndex = 0
    var isLocalBook = true
    var prevTextChapter: TextChapter? = null
    var curTextChapter: TextChapter? = null
    var nextTextChapter: TextChapter? = null
    private val loadingChapters = arrayListOf<Int>()
    private val loadingLock = "loadingLock"

    fun initData(intent: Intent) {
        inBookshelf = intent.getBooleanExtra("inBookshelf", true)
        val bookUrl = intent.getStringExtra("bookUrl")
        if (!bookUrl.isNullOrEmpty()) {
            execute {
                App.db.bookDao().getBook(bookUrl)?.let { book ->
                    bookData.postValue(book)
                    durChapterIndex = book.durChapterIndex
                    durPageIndex = book.durChapterPos
                    isLocalBook = book.origin == BookType.local
                    bookSource = App.db.bookSourceDao().getBookSource(book.origin)
                    bookSource?.let {
                        webBook = WebBook(it)
                    }
                    val count = App.db.bookChapterDao().getChapterCount(bookUrl)
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
                        callBack?.bookLoadFinish()
                    }
                }

            }
        }
    }

    private fun loadBookInfo(book: Book) {
        execute {
            webBook?.getBookInfo(book, this)
                ?.onSuccess {
                    loadChapterList(book)
                }
        }
    }

    private fun loadChapterList(book: Book) {
        execute {
            webBook?.getChapterList(book, this)
                ?.onSuccess(IO) { cList ->
                    if (!cList.isNullOrEmpty()) {
                        App.db.bookChapterDao().insert(*cList.toTypedArray())
                        chapterSize = cList.size
                        callBack?.bookLoadFinish()
                    } else {
                        toast(R.string.error_load_toc)
                    }
                }?.onError {
                    toast(R.string.error_load_toc)
                } ?: autoChangeSource()
        }
    }

    fun loadContent(book: Book, index: Int) {
        synchronized(loadingLock) {
            if (loadingChapters.contains(index)) return
            loadingChapters.add(index)
        }
        execute {
            App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                BookHelp.getContent(book, chapter)?.let {
                    callBack?.contentLoadFinish(chapter, it)
                    synchronized(loadingLock) {
                        loadingChapters.remove(index)
                    }
                } ?: download(book, chapter)
            } ?: synchronized(loadingLock) {
                loadingChapters.remove(index)
            }
        }.onError {
            synchronized(loadingLock) {
                loadingChapters.remove(index)
            }
        }
    }

    fun download(book: Book, index: Int) {
        synchronized(loadingLock) {
            if (loadingChapters.contains(index)) return
            loadingChapters.add(index)
        }
        execute {
            App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                if (!BookHelp.hasContent(book, chapter)) {
                    download(book, chapter)
                }
            }
        }.onError {
            synchronized(loadingLock) {
                loadingChapters.remove(index)
            }
        }
    }

    private fun download(book: Book, chapter: BookChapter) {
        webBook?.getContent(book, chapter, scope = this)
            ?.onSuccess(IO) { content ->
                content?.let {
                    BookHelp.saveContent(book, chapter, it)
                    callBack?.contentLoadFinish(chapter, it)
                    synchronized(loadingLock) {
                        loadingChapters.remove(chapter.index)
                    }
                }
            }?.onError {
                synchronized(loadingLock) {
                    loadingChapters.remove(chapter.index)
                }
            }
    }

    fun changeTo(book: Book) {
        execute {
            bookData.value?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
            bookData.postValue(book)
            bookSource = App.db.bookSourceDao().getBookSource(book.origin)
            if (book.tocUrl.isEmpty()) {
                loadBookInfo(book)
            } else {
                loadChapterList(book)
            }
        }
    }

    private fun autoChangeSource() {

    }

    fun openChapter(chapter: BookChapter) {
        if (chapter.index != durChapterIndex) {
            durChapterIndex = chapter.index
            durPageIndex = 0
        }
        callBack?.bookLoadFinish()
    }

    fun saveRead() {
        execute {
            bookData.value?.let { book ->
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = durPageIndex
                App.db.bookDao().update(book)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ReadAloudService.stop(context)
    }

    interface CallBack {
        fun bookLoadFinish()
        fun contentLoadFinish(bookChapter: BookChapter, content: String)
    }
}