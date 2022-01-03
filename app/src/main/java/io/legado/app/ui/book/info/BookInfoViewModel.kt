package io.legado.app.ui.book.info

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.ReadBook
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.postEvent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ensureActive

class BookInfoViewModel(application: Application) : BaseViewModel(application) {
    val bookData = MutableLiveData<Book>()
    val chapterListData = MutableLiveData<List<BookChapter>>()
    var durChapterIndex = 0
    var inBookshelf = false
    var bookSource: BookSource? = null
    private var changeSourceCoroutine: Coroutine<*>? = null

    fun initData(intent: Intent) {
        execute {
            val name = intent.getStringExtra("name") ?: ""
            val author = intent.getStringExtra("author") ?: ""
            val bookUrl = intent.getStringExtra("bookUrl") ?: ""
            appDb.bookDao.getBook(name, author)?.let { book ->
                inBookshelf = true
                setBook(book)
            } ?: let {
                val searchBook = appDb.searchBookDao.getSearchBook(bookUrl)
                    ?: appDb.searchBookDao.getFirstByNameAuthor(name, author)
                searchBook?.toBook()?.let { book ->
                    setBook(book)
                }
            }
        }
    }

    fun refreshData(intent: Intent) {
        execute {
            val name = intent.getStringExtra("name") ?: ""
            val author = intent.getStringExtra("author") ?: ""
            appDb.bookDao.getBook(name, author)?.let { book ->
                setBook(book)
            }
        }
    }

    private fun setBook(book: Book) {
        durChapterIndex = book.durChapterIndex
        bookData.postValue(book)
        bookSource = if (book.isLocalBook()) {
            null
        } else {
            appDb.bookSourceDao.getBookSource(book.origin)
        }
        if (book.tocUrl.isEmpty()) {
            loadBookInfo(book)
        } else {
            val chapterList = appDb.bookChapterDao.getChapterList(book.bookUrl)
            if (chapterList.isNotEmpty()) {
                chapterListData.postValue(chapterList)
            } else {
                loadChapter(book)
            }
        }
    }

    fun loadBookInfo(
        book: Book,
        canReName: Boolean = true,
        scope: CoroutineScope = viewModelScope,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null,
    ) {
        execute(scope) {
            if (book.isLocalBook()) {
                loadChapter(book, scope, changeDruChapterIndex)
            } else {
                bookSource?.let { bookSource ->
                    WebBook.getBookInfo(this, bookSource, book, canReName = canReName)
                        .onSuccess(IO) {
                            bookData.postValue(book)
                            if (inBookshelf) {
                                appDb.bookDao.update(book)
                            }
                            loadChapter(it, scope, changeDruChapterIndex)
                        }.onError {
                            AppLog.put("获取数据信息失败\n${it.localizedMessage}", it)
                            context.toastOnUi(R.string.error_get_book_info)
                        }
                } ?: let {
                    chapterListData.postValue(emptyList())
                    context.toastOnUi(R.string.error_no_source)
                }
            }
        }
    }

    private fun loadChapter(
        book: Book,
        scope: CoroutineScope = viewModelScope,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null,
    ) {
        execute(scope) {
            if (book.isLocalBook()) {
                LocalBook.getChapterList(book).let {
                    appDb.bookDao.update(book)
                    appDb.bookChapterDao.insert(*it.toTypedArray())
                    chapterListData.postValue(it)
                }
            } else {
                bookSource?.let { bookSource ->
                    WebBook.getChapterList(this, bookSource, book)
                        .onSuccess(IO) {
                            if (inBookshelf) {
                                appDb.bookDao.update(book)
                                appDb.bookChapterDao.insert(*it.toTypedArray())
                            }
                            if (changeDruChapterIndex == null) {
                                chapterListData.postValue(it)
                            } else {
                                changeDruChapterIndex(it)
                            }
                        }.onError {
                            chapterListData.postValue(emptyList())
                            AppLog.put("获取目录失败\n${it.localizedMessage}", it)
                            context.toastOnUi(R.string.error_get_chapter_list)
                        }
                } ?: let {
                    chapterListData.postValue(emptyList())
                    context.toastOnUi(R.string.error_no_source)
                }
            }
        }.onError {
            context.toastOnUi("LoadTocError:${it.localizedMessage}")
        }
    }

    fun loadGroup(groupId: Long, success: ((groupNames: String?) -> Unit)) {
        execute {
            appDb.bookGroupDao.getGroupNames(groupId).joinToString(",")
        }.onSuccess {
            success.invoke(it)
        }
    }

    fun changeTo(source: BookSource, newBook: Book) {
        changeSourceCoroutine?.cancel()
        changeSourceCoroutine = execute {
            var oldTocSize: Int = newBook.totalChapterNum
            if (inBookshelf) {
                bookData.value?.let {
                    oldTocSize = it.totalChapterNum
                    it.changeTo(newBook)
                }
            }
            bookData.postValue(newBook)
            bookSource = source
            if (newBook.tocUrl.isEmpty()) {
                loadBookInfo(newBook, false, this) {
                    ensureActive()
                    upChangeDurChapterIndex(newBook, oldTocSize, it)
                }
            } else {
                loadChapter(newBook, this) {
                    ensureActive()
                    upChangeDurChapterIndex(newBook, oldTocSize, it)
                }
            }
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, newBook.bookUrl)
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
                appDb.bookDao.update(book)
                appDb.bookChapterDao.insert(*chapters.toTypedArray())
            }
            bookData.postValue(book)
            chapterListData.postValue(chapters)
        }
    }

    fun topBook() {
        execute {
            bookData.value?.let { book ->
                val minOrder = appDb.bookDao.minOrder
                book.order = minOrder - 1
                book.durChapterTime = System.currentTimeMillis()
                appDb.bookDao.update(book)
            }
        }
    }

    fun saveBook(success: (() -> Unit)? = null) {
        execute {
            bookData.value?.let { book ->
                if (book.order == 0) {
                    book.order = appDb.bookDao.maxOrder + 1
                }
                appDb.bookDao.getBook(book.name, book.author)?.let {
                    book.durChapterPos = it.durChapterPos
                    book.durChapterTitle = it.durChapterTitle
                }
                book.save()
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
                appDb.bookChapterDao.insert(*it.toTypedArray())
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun addToBookshelf(success: (() -> Unit)?) {
        execute {
            bookData.value?.let { book ->
                if (book.order == 0) {
                    book.order = appDb.bookDao.maxOrder + 1
                }
                appDb.bookDao.getBook(book.name, book.author)?.let {
                    book.durChapterPos = it.durChapterPos
                    book.durChapterTitle = it.durChapterTitle
                }
                book.save()
            }
            chapterListData.value?.let {
                appDb.bookChapterDao.insert(*it.toTypedArray())
            }
            inBookshelf = true
        }.onSuccess {
            success?.invoke()
        }
    }

    fun delBook(deleteOriginal: Boolean = false, success: (() -> Unit)? = null) {
        execute {
            bookData.value?.let {
                Book.delete(it)
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
            context.toastOnUi(R.string.clear_cache_success)
        }.onError {
            context.toastOnUi("清理缓存出错\n${it.localizedMessage}")
        }
    }

    fun upEditBook() {
        bookData.value?.let {
            appDb.bookDao.getBook(it.bookUrl)?.let { book ->
                bookData.postValue(book)
            }
        }
    }
}