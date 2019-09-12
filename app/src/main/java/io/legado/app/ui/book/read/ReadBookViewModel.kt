package io.legado.app.ui.book.read

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
import io.legado.app.help.ReadAloud
import io.legado.app.model.WebBook
import io.legado.app.ui.widget.page.TextChapter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadBookViewModel(application: Application) : BaseViewModel(application) {
    var inBookshelf = false
    var bookData = MutableLiveData<Book>()
    val chapterListFinish = MutableLiveData<Boolean>()
    var chapterSize = 0
    var callBack: CallBack? = null
    var durChapterIndex = 0
    var durPageIndex = 0
    var isLocalBook = true
    var prevTextChapter: TextChapter? = null
    var curTextChapter: TextChapter? = null
    var nextTextChapter: TextChapter? = null
    var webBook: WebBook? = null
    private val loadingChapters = arrayListOf<Int>()
    private val loadingLock = "loadingLock"

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
        }.onError { it.printStackTrace() }
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
                ?.onSuccess(IO) { cList ->
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
                    it.printStackTrace()
                } ?: autoChangeSource()
        }
    }

    fun moveToNextChapter() {
        durChapterIndex++
        prevTextChapter = curTextChapter
        curTextChapter = nextTextChapter
        nextTextChapter = null
        bookData.value?.let {
            if (curTextChapter == null) {
                loadContent(it, durChapterIndex)
            } else {
                callBack?.upContent()
            }
            loadContent(it, durChapterIndex.plus(1))
            launch(IO) {
                for (i in 2..10) {
                    delay(100)
                    bookData.value?.let { book ->
                        download(book, durChapterIndex + i)
                    }
                }
            }
        }
    }

    fun moveToPrevChapter() {
        durChapterIndex--
        nextTextChapter = curTextChapter
        curTextChapter = prevTextChapter
        prevTextChapter = null
        bookData.value?.let {
            if (curTextChapter == null) {
                loadContent(it, durChapterIndex)
            } else {
                callBack?.upContent()
            }
            loadContent(it, durChapterIndex.minus(1))
            launch(IO) {
                for (i in -5..-2) {
                    delay(100)
                    bookData.value?.let { book ->
                        download(book, durChapterIndex + i)
                    }
                }
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
                contentLoadFinish(chapter, it.localizedMessage)
                removeLoading(chapter.index)
            }
    }

    private fun addLoading(index: Int): Boolean {
        synchronized(loadingLock) {
            if (loadingChapters.contains(index)) return false
            loadingChapters.add(index)
            return true
        }
    }

    private fun removeLoading(index: Int) {
        synchronized(loadingLock) {
            loadingChapters.remove(index)
        }
    }

    private fun contentLoadFinish(chapter: BookChapter, content: String) {
        execute {
            if (chapter.index in durChapterIndex - 1..durChapterIndex + 1) {
                val c = BookHelp.disposeContent(
                    bookData.value?.name ?: "",
                    webBook?.bookSource?.bookSourceUrl,
                    content,
                    bookData.value?.useReplaceRule ?: true
                )
                callBack?.contentLoadFinish(chapter, c)
            }
        }
    }

    fun changeTo(book: Book) {
        execute {
            bookData.value?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
            prevTextChapter = null
            curTextChapter = null
            nextTextChapter = null
            withContext(Main) {
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

    private fun autoChangeSource() {

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
            chapterListFinish.postValue(true)
        }
    }

    fun openChapter(chapter: BookChapter) {
        prevTextChapter = null
        curTextChapter = null
        nextTextChapter = null
        if (chapter.index != durChapterIndex) {
            durChapterIndex = chapter.index
            durPageIndex = 0
        }
        saveRead()
        chapterListFinish.postValue(true)
    }

    fun saveRead(book: Book? = bookData.value) {
        execute {
            book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = durPageIndex
                curTextChapter?.let {
                    book.durChapterTitle = it.title
                }
                App.db.bookDao().update(book)
            }
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            bookData.value?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun upBookSource() {
        execute {
            bookData.value?.let { book ->
                App.db.bookSourceDao().getBookSource(book.origin)?.let {
                    webBook = WebBook(it)
                }
            }
        }
    }

    fun refreshContent(book: Book) {
        execute {
            App.db.bookChapterDao().getChapter(book.bookUrl, durChapterIndex)?.let { chapter ->
                BookHelp.delContent(book, chapter)
                loadContent(book, durChapterIndex)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ReadAloud.stop(context)
    }

    interface CallBack {
        fun contentLoadFinish(bookChapter: BookChapter, content: String)
        fun upContent()
    }
}