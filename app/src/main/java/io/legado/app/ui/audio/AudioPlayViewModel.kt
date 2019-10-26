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
import io.legado.app.model.WebBook
import io.legado.app.ui.book.read.ReadBookViewModel
import kotlinx.coroutines.Dispatchers

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {
    var inBookshelf = false
    var bookData = MutableLiveData<Book>()
    val chapterListFinish = MutableLiveData<Boolean>()
    var chapterSize = 0
    var callBack: ReadBookViewModel.CallBack? = null
    var durChapterIndex = 0
    var durPageIndex = 0
    var isLocalBook = true
    var webBook: WebBook? = null

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
                    chapterListFinish.postValue(true)
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
                            chapterListFinish.postValue(true)
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
}