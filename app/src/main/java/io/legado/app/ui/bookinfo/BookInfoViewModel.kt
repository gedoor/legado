package io.legado.app.ui.bookinfo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers.IO

class BookInfoViewModel(application: Application) : BaseViewModel(application) {

    val bookData = MutableLiveData<Book>()
    val chapterListData = MutableLiveData<List<BookChapter>>()
    val isLoadingData = MutableLiveData<Boolean>()
    var durChapterIndex = 0
    var inBookshelf = false

    fun loadBook(intent: Intent) {
        execute {
            intent.getStringExtra("bookUrl")?.let {
                App.db.bookDao().getBook(it)?.let { book ->
                    inBookshelf = true
                    durChapterIndex = book.durChapterIndex
                    bookData.postValue(book)
                    val chapterList = App.db.bookChapterDao().getChapterList(it)
                    if (chapterList.isNotEmpty()) {
                        chapterListData.postValue(chapterList)
                        isLoadingData.postValue(false)
                    } else {
                        loadChapter(book)
                    }
                }
            } ?: intent.getStringExtra("searchBookUrl")?.let {
                App.db.searchBookDao().getSearchBook(it)?.toBook()?.let { book ->
                    durChapterIndex = book.durChapterIndex
                    bookData.postValue(book)
                    if (book.tocUrl.isEmpty()) {
                        loadBookInfo(book)
                    } else {
                        loadChapter(book)
                    }
                }
            }
        }
    }

    fun loadBookInfo(book: Book) {
        execute {
            isLoadingData.postValue(true)
            App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                WebBook(bookSource).getBookInfo(book, this)
                    .onSuccess {
                        it?.let { loadChapter(it) }
                    }.onError {
                        toast(R.string.error_get_book_info)
                    }
            } ?: toast(R.string.error_no_source)
        }
    }

    fun loadChapter(book: Book) {
        execute {
            isLoadingData.postValue(true)
            App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                WebBook(bookSource).getChapterList(book, this)
                    .onSuccess(IO) {
                        it?.let {
                            if (it.isNotEmpty()) {
                                if (inBookshelf) {
                                    App.db.bookChapterDao().insert(*it.toTypedArray())
                                }
                                chapterListData.postValue(it)
                                isLoadingData.postValue(false)
                            }
                        }
                    }.onError {
                        toast(R.string.error_get_chapter_list)
                    }
            } ?: toast(R.string.error_no_source)
        }
    }

    fun changeTo(book: Book) {
        execute {
            if (inBookshelf) {
                bookData.value?.let {
                    App.db.bookDao().delete(it.bookUrl)
                }
                App.db.bookDao().insert(book)
            }
            bookData.postValue(book)
            if (book.tocUrl.isEmpty()) {
                loadBookInfo(book)
            } else {
                loadChapter(book)
            }
        }
    }

    fun saveBook(success: (() -> Unit)?) {
        execute {
            bookData.value?.let { book ->
                App.db.bookDao().insert(book)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun saveChapterList(success: (() -> Unit)?) {
        execute {
            chapterListData.value?.let {
                App.db.bookChapterDao().insert(*it.toTypedArray())
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun addToBookshelf(success: (() -> Unit)?) {
        execute {
            bookData.value?.let { book ->
                App.db.bookDao().insert(book)
            }
            chapterListData.value?.let {
                App.db.bookChapterDao().insert(*it.toTypedArray())
            }
            inBookshelf = true
        }.onSuccess {
            success?.invoke()
        }
    }

    fun delBook(success: (() -> Unit)?) {
        execute {
            bookData.value?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
            inBookshelf = false
        }.onSuccess {
            success?.invoke()
        }
    }
}