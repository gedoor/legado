package io.legado.app.ui.audio

import android.app.Application
import android.content.Intent
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
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
            if (book?.bookUrl != bookUrl) {
                stop(context)
                inBookshelf = intent.getBooleanExtra("inBookshelf", true)
                book = if (!bookUrl.isNullOrEmpty()) {
                    App.db.bookDao.getBook(bookUrl)
                } else {
                    App.db.bookDao.lastReadBook
                }
                book?.let { book ->
                    titleData.postValue(book.name)
                    coverData.postValue(book.getDisplayCover())
                    durChapterIndex = book.durChapterIndex
                    durChapterPos = book.durChapterPos
                    durChapter = App.db.bookChapterDao.getChapter(book.bookUrl, durChapterIndex)
                    upDurChapter(book)
                    App.db.bookSourceDao.getBookSource(book.origin)?.let {
                        webBook = WebBook(it)
                    }
                    if (durChapter == null) {
                        if (book.tocUrl.isEmpty()) {
                            loadBookInfo(book)
                        } else {
                            loadChapterList(book)
                        }
                    }
                }
                saveRead()
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
                            App.db.bookChapterDao.insert(*cList.toTypedArray())
                        } else {
                            changeDruChapterIndex(cList)
                        }
                        AudioPlay.upDurChapter(book)
                    } else {
                        toast(R.string.error_load_toc)
                    }
                }?.onError {
                    toast(R.string.error_load_toc)
                }
        }
    }

    fun changeTo(book1: Book) {
        execute {
            var oldTocSize: Int = book1.totalChapterNum
            AudioPlay.book?.let {
                oldTocSize = it.totalChapterNum
                book1.order = it.order
                App.db.bookDao.delete(it)
            }
            App.db.bookDao.insert(book1)
            AudioPlay.book = book1
            App.db.bookSourceDao.getBookSource(book1.origin)?.let {
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
            App.db.bookDao.update(book)
            App.db.bookChapterDao.insert(*chapters.toTypedArray())
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                App.db.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}