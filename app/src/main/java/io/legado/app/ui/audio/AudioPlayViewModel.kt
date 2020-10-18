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

    fun initData(intent: Intent) {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl")
            if (AudioPlay.book?.bookUrl != bookUrl) {
                AudioPlay.stop(context)
                AudioPlay.inBookshelf = intent.getBooleanExtra("inBookshelf", true)
                AudioPlay.book = if (!bookUrl.isNullOrEmpty()) {
                    App.db.bookDao().getBook(bookUrl)
                } else {
                    App.db.bookDao().lastReadBook
                }
                AudioPlay.book?.let { book ->
                    AudioPlay.titleData.postValue(book.name)
                    AudioPlay.coverData.postValue(book.getDisplayCover())
                    AudioPlay.durChapterIndex = book.durChapterIndex
                    AudioPlay.durPageIndex = book.durChapterPos
                    App.db.bookSourceDao().getBookSource(book.origin)?.let {
                        AudioPlay.webBook = WebBook(it)
                    }
                    val count = App.db.bookChapterDao().getChapterCount(book.bookUrl)
                    if (count == 0) {
                        if (book.tocUrl.isEmpty()) {
                            loadBookInfo(book)
                        } else {
                            loadChapterList(book)
                        }
                    } else {
                        if (AudioPlay.durChapterIndex > count - 1) {
                            AudioPlay.durChapterIndex = count - 1
                        }
                        AudioPlay.chapterSize = count
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
            AudioPlay.webBook?.getBookInfo(book, this)
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
            AudioPlay.webBook?.getChapterList(book, this)
                ?.onSuccess(Dispatchers.IO) { cList ->
                    if (cList.isNotEmpty()) {
                        if (changeDruChapterIndex == null) {
                            App.db.bookChapterDao().insert(*cList.toTypedArray())
                            AudioPlay.chapterSize = cList.size
                        } else {
                            changeDruChapterIndex(cList)
                        }
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
                App.db.bookDao().delete(it)
            }
            App.db.bookDao().insert(book1)
            AudioPlay.book = book1
            App.db.bookSourceDao().getBookSource(book1.origin)?.let {
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
            App.db.bookDao().update(book)
            App.db.bookChapterDao().insert(*chapters.toTypedArray())
            AudioPlay.chapterSize = chapters.size
        }
    }

    fun saveRead() {
        execute {
            AudioPlay.book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = AudioPlay.durChapterIndex
                book.durChapterPos = AudioPlay.durPageIndex
                App.db.bookChapterDao().getChapter(book.bookUrl, book.durChapterIndex)?.let {
                    book.durChapterTitle = it.title
                }
                App.db.bookDao().update(book)
            }
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                App.db.bookDao().delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}