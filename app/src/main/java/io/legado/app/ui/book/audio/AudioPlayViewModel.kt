package io.legado.app.ui.book.audio

import android.app.Application
import android.content.Intent
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.book.removeType
import io.legado.app.model.AudioPlay
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.postEvent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {

    fun initData(intent: Intent) = AudioPlay.apply {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl")
            if (bookUrl != null && bookUrl != book?.bookUrl) {
                stop(context)
                inBookshelf = intent.getBooleanExtra("inBookshelf", true)
                book = appDb.bookDao.getBook(bookUrl)
                book?.let { book ->
                    titleData.postValue(book.name)
                    coverData.postValue(book.getDisplayCover())
                    durChapter = appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)
                    upDurChapter(book)
                    bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                    if (durChapter == null) {
                        if (book.tocUrl.isEmpty()) {
                            loadBookInfo(book)
                        } else {
                            loadChapterList(book)
                        }
                    }
                }
            }
            saveRead()
        }
    }

    private fun loadBookInfo(book: Book) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getBookInfo(this, it, book)
                    .onSuccess {
                        loadChapterList(book)
                    }
            }
        }
    }

    private fun loadChapterList(book: Book) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getChapterList(this, it, book)
                    .onSuccess(Dispatchers.IO) { cList ->
                        book.save()
                        appDb.bookChapterDao.insert(*cList.toTypedArray())
                        AudioPlay.upDurChapter(book)
                    }.onError {
                        context.toastOnUi(R.string.error_load_toc)
                    }
            }
        }
    }

    fun upSource() {
        execute {
            AudioPlay.book?.let { book ->
                AudioPlay.bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            }
        }
    }

    fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        execute {
            AudioPlay.book?.migrateTo(book, toc)
            book.removeType(BookType.updateError)
            AudioPlay.book?.delete()
            appDb.bookDao.insert(book)
            AudioPlay.book = book
            AudioPlay.bookSource = source
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            AudioPlay.upDurChapter(book)
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                appDb.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}