package io.legado.app.ui.book.info

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.help.ReadBook
import kotlinx.coroutines.Dispatchers.IO

class BookInfoViewModel(application: Application) : BaseViewModel(application) {
    val bookData = MutableLiveData<Book>()
    val chapterListData = MutableLiveData<List<BookChapter>>()
    var durChapterIndex = 0
    var inBookshelf = false

    fun initData(intent: Intent) {
        execute {
            val name = intent.getStringExtra("name") ?: ""
            val author = intent.getStringExtra("author") ?: ""
            App.db.bookDao.getBook(name, author)?.let { book ->
                inBookshelf = true
                setBook(book)
            } ?: App.db.searchBookDao.getFirstByNameAuthor(name, author)?.toBook()?.let { book ->
                setBook(book)
            }
        }
    }

    private fun setBook(book: Book) {
        durChapterIndex = book.durChapterIndex
        bookData.postValue(book)
        if (book.tocUrl.isEmpty()) {
            loadBookInfo(book)
        } else {
            val chapterList = App.db.bookChapterDao.getChapterList(book.bookUrl)
            if (chapterList.isNotEmpty()) {
                chapterListData.postValue(chapterList)
            } else {
                loadChapter(book)
            }
        }
    }

    fun loadBookInfo(
        book: Book, canReName: Boolean = true,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null,
    ) {
        execute {
            if (book.isLocalBook()) {
                loadChapter(book, changeDruChapterIndex)
            } else {
                App.db.bookSourceDao.getBookSource(book.origin)?.let { bookSource ->
                    WebBook(bookSource).getBookInfo(this, book, canReName = canReName)
                        .onSuccess(IO) {
                            bookData.postValue(book)
                            if (inBookshelf) {
                                App.db.bookDao.update(book)
                            }
                            loadChapter(it, changeDruChapterIndex)
                        }.onError {
                            toast(R.string.error_get_book_info)
                        }
                } ?: let {
                    chapterListData.postValue(null)
                    toast(R.string.error_no_source)
                }
            }
        }
    }

    private fun loadChapter(
        book: Book,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null
    ) {
        execute {
            if (book.isLocalBook()) {
                LocalBook.getChapterList(book).let {
                    App.db.bookDao.update(book)
                    App.db.bookChapterDao.insert(*it.toTypedArray())
                    chapterListData.postValue(it)
                }
            } else {
                App.db.bookSourceDao.getBookSource(book.origin)?.let { bookSource ->
                    WebBook(bookSource).getChapterList(this, book)
                        .onSuccess(IO) {
                            if (it.isNotEmpty()) {
                                if (inBookshelf) {
                                    App.db.bookDao.update(book)
                                    App.db.bookChapterDao.insert(*it.toTypedArray())
                                }
                                if (changeDruChapterIndex == null) {
                                    chapterListData.postValue(it)
                                } else {
                                    changeDruChapterIndex(it)
                                }
                            } else {
                                toast(R.string.chapter_list_empty)
                            }
                        }.onError {
                            chapterListData.postValue(null)
                            toast(R.string.error_get_chapter_list)
                        }
                } ?: let {
                    chapterListData.postValue(null)
                    toast(R.string.error_no_source)
                }
            }
        }.onError {
            toast("LoadTocError:${it.localizedMessage}")
        }
    }

    fun loadGroup(groupId: Long, success: ((groupNames: String?) -> Unit)) {
        execute {
            App.db.bookGroupDao.getGroupNames(groupId).joinToString(",")
        }.onSuccess {
            success.invoke(it)
        }
    }

    fun changeTo(newBook: Book) {
        execute {
            var oldTocSize: Int = newBook.totalChapterNum
            if (inBookshelf) {
                bookData.value?.let {
                    oldTocSize = it.totalChapterNum
                    it.changeTo(newBook)
                }
            }
            bookData.postValue(newBook)
            if (newBook.tocUrl.isEmpty()) {
                loadBookInfo(newBook, false) {
                    upChangeDurChapterIndex(newBook, oldTocSize, it)
                }
            } else {
                loadChapter(newBook) {
                    upChangeDurChapterIndex(newBook, oldTocSize, it)
                }
            }
        }
    }

    private fun upChangeDurChapterIndex(
        book: Book,
        oldTocSize: Int,
        chapters: List<BookChapter>
    ) {
        execute {
            book.durChapterIndex = BookHelp.getDurChapter(
                book.durChapterIndex,
                oldTocSize,
                book.durChapterTitle,
                chapters
            )
            book.durChapterTitle = chapters[book.durChapterIndex].title
            if (inBookshelf) {
                App.db.bookDao.update(book)
                App.db.bookChapterDao.insert(*chapters.toTypedArray())
            }
            bookData.postValue(book)
            chapterListData.postValue(chapters)
        }
    }

    fun saveBook(success: (() -> Unit)? = null) {
        execute {
            bookData.value?.let { book ->
                if (book.order == 0) {
                    book.order = App.db.bookDao.maxOrder + 1
                }
                App.db.bookDao.getBook(book.name, book.author)?.let {
                    book.durChapterPos = it.durChapterPos
                    book.durChapterTitle = it.durChapterTitle
                }
                App.db.bookDao.insert(book)
                if (ReadBook.book?.name == book.name && ReadBook.book?.author == book.author) {
                    ReadBook.book = book
                }
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun saveChapterList(success: (() -> Unit)?) {
        execute {
            chapterListData.value?.let {
                App.db.bookChapterDao.insert(*it.toTypedArray())
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun addToBookshelf(success: (() -> Unit)?) {
        execute {
            bookData.value?.let { book ->
                if (book.order == 0) {
                    book.order = App.db.bookDao.maxOrder + 1
                }
                App.db.bookDao.getBook(book.name, book.author)?.let {
                    book.durChapterPos = it.durChapterPos
                    book.durChapterTitle = it.durChapterTitle
                }
                App.db.bookDao.insert(book)
            }
            chapterListData.value?.let {
                App.db.bookChapterDao.insert(*it.toTypedArray())
            }
            inBookshelf = true
        }.onSuccess {
            success?.invoke()
        }
    }

    fun delBook(deleteOriginal: Boolean = false, success: (() -> Unit)? = null) {
        execute {
            bookData.value?.let {
                it.delete()
                inBookshelf = false
                if (it.isLocalBook()) {
                    LocalBook.deleteBook(it, deleteOriginal)
                }
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache(bookData.value!!)
        }.onSuccess {
            toast(R.string.clear_cache_success)
        }.onError {
            toast(it.stackTraceToString())
        }
    }

    fun upEditBook() {
        bookData.value?.let {
            App.db.bookDao.getBook(it.bookUrl)?.let { book ->
                bookData.postValue(book)
            }
        }
    }
}