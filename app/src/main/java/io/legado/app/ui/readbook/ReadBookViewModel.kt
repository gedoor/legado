package io.legado.app.ui.readbook

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MediatorLiveData
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
import io.legado.app.ui.widget.page.TextChapter
import kotlinx.coroutines.Dispatchers.IO

class ReadBookViewModel(application: Application) : BaseViewModel(application) {

    var bookData = MutableLiveData<Book>()
    var chapterMaxIndex = MediatorLiveData<Int>()
    var bookSource: BookSource? = null
    var webBook: WebBook? = null
    var callBack: CallBack? = null
    var durChapterIndex = 0
    var isLocalBook = true
    var prevTextChapter: TextChapter? = null
    var curTextChapter: TextChapter? = null
    var nextTextChapter: TextChapter? = null
    private val loadingChapters = arrayListOf<Int>()
    private val loadingLock = "loadingLock"

    fun initData(intent: Intent) {
        val bookUrl = intent.getStringExtra("bookUrl")
        if (!bookUrl.isNullOrEmpty()) {
            execute {
                App.db.bookDao().getBook(bookUrl)?.let { book ->
                    bookData.postValue(book)
                    durChapterIndex = book.durChapterIndex
                    isLocalBook = book.origin == BookType.local
                    bookSource = App.db.bookSourceDao().getBookSource(book.origin)
                    bookSource?.let {
                        webBook = WebBook(it)
                    }
                    val count = App.db.bookChapterDao().getChapterCount(bookUrl)
                    if (count == 0) {
                        webBook?.getChapterList(book)
                            ?.onSuccess(IO) { cList ->
                                if (!cList.isNullOrEmpty()) {
                                    App.db.bookChapterDao().insert(*cList.toTypedArray())
                                    chapterMaxIndex.postValue(cList.size)
                                } else {
                                    toast(R.string.load_toc_error)
                                }
                            }?.onError {
                                toast(R.string.load_toc_error)
                            } ?: autoChangeSource()
                    } else {
                        chapterMaxIndex.postValue(count)
                    }
                }

            }
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
                    callBack?.loadContentFinish(chapter, it)
                    synchronized(loadingLock) {
                        loadingChapters.remove(index)
                    }
                } ?: download(book, chapter)
            }
        }
    }

    fun downLoad(book: Book, index: Int) {
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
        }
    }

    private fun download(book: Book, chapter: BookChapter) {
        webBook?.getContent(book, chapter)
            ?.onSuccess(IO) { content ->
                content?.let {
                    BookHelp.saveContent(book, chapter, it)
                    callBack?.loadContentFinish(chapter, it)
                    synchronized(loadingLock) {
                        loadingChapters.remove(chapter.index)
                    }
                }
            }
    }

    private fun autoChangeSource() {

    }

    interface CallBack {
        fun loadContentFinish(bookChapter: BookChapter, content: String)
    }
}