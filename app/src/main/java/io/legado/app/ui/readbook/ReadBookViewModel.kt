package io.legado.app.ui.readbook

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MediatorLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers.IO

class ReadBookViewModel(application: Application) : BaseViewModel(application) {

    var book: Book? = null
    var bookSource: BookSource? = null
    var chapterMaxIndex = MediatorLiveData<Int>()
    var webBook: WebBook? = null

    fun initData(intent: Intent) {
        val bookUrl = intent.getStringExtra("bookUrl")
        if (!bookUrl.isNullOrEmpty()) {
            execute {
                book = App.db.bookDao().getBook(bookUrl)
                book?.let { book ->
                    bookSource = App.db.bookSourceDao().getBookSource(book.origin)
                    bookSource?.let {
                        webBook = WebBook(it)
                    }
                    val count = App.db.bookChapterDao().getChapterCount(bookUrl)
                    if (count == 0) {
                        webBook?.getChapterList(book)
                            ?.onSuccess(IO) { cList ->
                                cList?.let {
                                    App.db.bookChapterDao().insert(*cList.toTypedArray())
                                    chapterMaxIndex.postValue(cList.size)
                                }
                            }
                    } else {
                        chapterMaxIndex.postValue(count)
                    }
                }

            }
        }
    }


    fun loadContent(book: Book, index: Int) {
        App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
            BookHelp.getContent(book, chapter)?.let {

            } ?: download(book, chapter)
        }
    }

    private fun download(book: Book, chapter: BookChapter) {
        webBook?.getContent(book, chapter)
            ?.onSuccess(IO) { content ->
                content?.let {
                    BookHelp.saveContent(book, chapter, it)
                }
            }
    }
}