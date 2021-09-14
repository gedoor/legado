package io.legado.app.ui.book.audio

import android.app.Application
import android.content.Intent
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookHelp
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
                    saveRead(book)
                }
            }
        }
    }

    private fun loadBookInfo(
        book: Book,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null
    ) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getBookInfo(this, it, book)
                    .onSuccess {
                        loadChapterList(book, changeDruChapterIndex)
                    }
            }
        }
    }

    private fun loadChapterList(
        book: Book,
        changeDruChapterIndex: ((chapters: List<BookChapter>) -> Unit)? = null
    ) {
        execute {
            AudioPlay.bookSource?.let {
                WebBook.getChapterList(this, it, book)
                    .onSuccess(Dispatchers.IO) { cList ->
                        if (changeDruChapterIndex == null) {
                            appDb.bookChapterDao.insert(*cList.toTypedArray())
                        } else {
                            changeDruChapterIndex(cList)
                        }
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

    fun changeTo(source: BookSource, book: Book) {
        execute {
            var oldTocSize: Int = book.totalChapterNum
            AudioPlay.book?.let {
                oldTocSize = it.totalChapterNum
                book.order = it.order
                appDb.bookDao.delete(it)
            }
            appDb.bookDao.insert(book)
            AudioPlay.book = book
            AudioPlay.bookSource = source
            if (book.tocUrl.isEmpty()) {
                loadBookInfo(book) { upChangeDurChapterIndex(book, oldTocSize, it) }
            } else {
                loadChapterList(book) { upChangeDurChapterIndex(book, oldTocSize, it) }
            }
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
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
            appDb.bookDao.update(book)
            appDb.bookChapterDao.insert(*chapters.toTypedArray())
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