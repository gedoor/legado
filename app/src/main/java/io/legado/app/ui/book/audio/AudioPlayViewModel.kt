package io.legado.app.ui.book.audio

import android.app.Application
import android.content.Intent
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.help.AudioPlay
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
                    durChapterIndex = book.durChapterIndex
                    durChapterPos = book.durChapterPos
                    durChapter = appDb.bookChapterDao.getChapter(book.bookUrl, durChapterIndex)
                    upDurChapter(book)
                    appDb.bookSourceDao.getBookSource(book.origin)?.let {
                        webBook = WebBook(it)
                    }
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
            AudioPlay.webBook?.getBookInfo(this, book)
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
            AudioPlay.webBook?.getChapterList(this, book)
                ?.onSuccess(Dispatchers.IO) { cList ->
                    if (cList.isNotEmpty()) {
                        if (changeDruChapterIndex == null) {
                            appDb.bookChapterDao.insert(*cList.toTypedArray())
                        } else {
                            changeDruChapterIndex(cList)
                        }
                        AudioPlay.upDurChapter(book)
                    } else {
                        toastOnUi(R.string.error_load_toc)
                    }
                }?.onError {
                    toastOnUi(R.string.error_load_toc)
                }
        }
    }

    fun changeTo(book1: Book) {
        execute {
            var oldTocSize: Int = book1.totalChapterNum
            AudioPlay.book?.let {
                oldTocSize = it.totalChapterNum
                book1.order = it.order
                appDb.bookDao.delete(it)
            }
            appDb.bookDao.insert(book1)
            AudioPlay.book = book1
            appDb.bookSourceDao.getBookSource(book1.origin)?.let {
                AudioPlay.webBook = WebBook(it)
            }
            if (book1.tocUrl.isEmpty()) {
                loadBookInfo(book1) { upChangeDurChapterIndex(book1, oldTocSize, it) }
            } else {
                loadChapterList(book1) { upChangeDurChapterIndex(book1, oldTocSize, it) }
            }
        }
    }

    private fun upChangeDurChapterIndex(
        book: Book,
        oldTocSize: Int,
        chapters: List<BookChapter>
    ) {
        execute {
            AudioPlay.durChapterIndex = BookHelp.getDurChapter(
                book.durChapterIndex,
                oldTocSize,
                book.durChapterTitle,
                chapters
            )
            book.durChapterIndex = AudioPlay.durChapterIndex
            book.durChapterTitle = chapters[AudioPlay.durChapterIndex].title
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