package io.legado.app.ui.book.read

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.ui.widget.page.TextChapter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast

class ReadBookViewModel {
    var titleDate = MutableLiveData<String>()
    var book: Book? = null
    var inBookshelf = false
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

    fun initData(intent: Intent) {
        Coroutine.async {
            inBookshelf = intent.getBooleanExtra("inBookshelf", true)
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
        this.book = book
        titleDate.postValue(book.name)
        durChapterIndex = book.durChapterIndex
        durPageIndex = book.durChapterPos
        isLocalBook = book.origin == BookType.local
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
        if (inBookshelf) {
            saveRead()
        }
    }

    private fun loadBookInfo(
        book: Book,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null
    ) {
        Coroutine.async {
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
        Coroutine.async {
            webBook?.getChapterList(book, this)
                ?.onSuccess(IO) { cList ->
                    if (!cList.isNullOrEmpty()) {
                        if (changeDruChapterIndex == null) {
                            App.db.bookChapterDao().insert(*cList.toTypedArray())
                            chapterSize = cList.size
                            callBack?.loadContent()
                        } else {
                            changeDruChapterIndex(cList)
                        }
                    } else {
                        App.INSTANCE.toast(R.string.error_load_toc)
                    }
                }?.onError {
                    App.INSTANCE.toast(R.string.error_load_toc)
                } ?: autoChangeSource()
        }
    }

    fun moveToNextChapter(upContent: Boolean) {
        durChapterIndex++
        prevTextChapter = curTextChapter
        curTextChapter = nextTextChapter
        nextTextChapter = null
        book?.let {
            if (curTextChapter == null) {
                loadContent(durChapterIndex)
            } else if (upContent) {
                callBack?.upContent()
            }
            loadContent(durChapterIndex.plus(1))
            GlobalScope.launch(IO) {
                for (i in 2..10) {
                    delay(100)
                    download(durChapterIndex + i)
                }
            }
        }
    }

    fun moveToPrevChapter(upContent: Boolean) {
        durChapterIndex--
        nextTextChapter = curTextChapter
        curTextChapter = prevTextChapter
        prevTextChapter = null
        book?.let {
            if (curTextChapter == null) {
                loadContent(durChapterIndex)
            } else if (upContent) {
                callBack?.upContent()
            }
            loadContent(durChapterIndex.minus(1))
            GlobalScope.launch(IO) {
                for (i in -5..-2) {
                    delay(100)
                    download(durChapterIndex + i)
                }
            }
        }
    }

    fun loadContent(index: Int) {
        book?.let { book ->
            if (addLoading(index)) {
                Coroutine.async {
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
        book?.let { book ->
            if (addLoading(index)) {
                Coroutine.async {
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
        book?.let { book ->
            webBook?.getContent(book, chapter)
                ?.onSuccess(IO) { content ->
                    if (content.isNullOrEmpty()) {
                        contentLoadFinish(chapter, App.INSTANCE.getString(R.string.content_empty))
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
        Coroutine.async {
            if (chapter.index in durChapterIndex - 1..durChapterIndex + 1) {
                val c = BookHelp.disposeContent(
                    chapter.title,
                    book!!.name,
                    webBook?.bookSource?.bookSourceUrl,
                    content,
                    book!!.useReplaceRule
                )
                callBack?.contentLoadFinish(chapter, c)
            }
        }
    }

    fun changeTo(book1: Book) {
        Coroutine.async {
            book?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
            prevTextChapter = null
            curTextChapter = null
            nextTextChapter = null
            withContext(Main) {
                callBack?.upContent()
            }
            App.db.bookDao().insert(book1)
            book = book1
            App.db.bookSourceDao().getBookSource(book1.origin)?.let {
                webBook = WebBook(it)
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
        Coroutine.async {
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

    fun openChapter(index: Int) {
        prevTextChapter = null
        curTextChapter = null
        nextTextChapter = null
        callBack?.upContent()
        if (index != durChapterIndex) {
            durChapterIndex = index
            durPageIndex = 0
        }
        saveRead()
        callBack?.loadContent()
    }

    fun saveRead() {
        Coroutine.async {
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
        Coroutine.async {
            book?.let {
                App.db.bookDao().delete(it.bookUrl)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun upBookSource() {
        Coroutine.async {
            book?.let { book ->
                App.db.bookSourceDao().getBookSource(book.origin)?.let {
                    webBook = WebBook(it)
                }
            }
        }
    }

    fun refreshContent(book: Book) {
        Coroutine.async {
            App.db.bookChapterDao().getChapter(book.bookUrl, durChapterIndex)?.let { chapter ->
                BookHelp.delContent(book, chapter)
                loadContent(durChapterIndex)
            }
        }
    }

    interface CallBack {
        fun loadContent()
        fun contentLoadFinish(bookChapter: BookChapter, content: String)
        fun upContent()
    }
}