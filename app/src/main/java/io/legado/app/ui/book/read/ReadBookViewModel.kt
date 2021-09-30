package io.legado.app.ui.book.read

import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookProgress
import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.ContentProcessor
import io.legado.app.help.storage.AppWebDav
import io.legado.app.model.BookRead
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.ReadAloud
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.msg
import io.legado.app.utils.postEvent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO

class ReadBookViewModel(application: Application) : BaseViewModel(application) {
    var isInitFinish = false
    var searchContentQuery = ""

    fun initData(intent: Intent) {
        execute {
            BookRead.inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            val bookUrl = intent.getStringExtra("bookUrl")
            val book = when {
                bookUrl.isNullOrEmpty() -> appDb.bookDao.lastReadBook
                else -> appDb.bookDao.getBook(bookUrl)
            } ?: BookRead.book
            when {
                book != null -> initBook(book)
                else -> BookRead.upMsg(context.getString(R.string.no_book))
            }
        }.onFinally {
            BookRead.saveRead()
        }
    }

    private fun initBook(book: Book) {
        if (BookRead.book?.bookUrl != book.bookUrl) {
            BookRead.resetData(book)
            isInitFinish = true
            if (BookRead.chapterSize == 0) {
                if (book.tocUrl.isEmpty()) {
                    loadBookInfo(book)
                } else {
                    loadChapterList(book)
                }
            } else {
                if (BookRead.durChapterIndex > BookRead.chapterSize - 1) {
                    BookRead.durChapterIndex = BookRead.chapterSize - 1
                }
                BookRead.loadContent(resetPageOffset = true)
            }
            syncBookProgress(book)
        } else {
            BookRead.book = book
            if (BookRead.durChapterIndex != book.durChapterIndex) {
                BookRead.durChapterIndex = book.durChapterIndex
                BookRead.durChapterPos = book.durChapterPos
                BookRead.clearTextChapter()
            }
            BookRead.callBack?.upMenuView()
            BookRead.upWebBook(book)
            isInitFinish = true
            BookRead.chapterSize = appDb.bookChapterDao.getChapterCount(book.bookUrl)
            if (BookRead.chapterSize == 0) {
                if (book.tocUrl.isEmpty()) {
                    loadBookInfo(book)
                } else {
                    loadChapterList(book)
                }
            } else {
                if (BookRead.curTextChapter != null) {
                    BookRead.callBack?.upContent(resetPageOffset = false)
                } else {
                    BookRead.loadContent(resetPageOffset = true)
                }
            }
            if (!BaseReadAloudService.isRun) {
                syncBookProgress(book)
            }
        }
        if (!book.isLocalBook() && BookRead.bookSource == null) {
            autoChangeSource(book.name, book.author)
            return
        }
    }

    private fun loadBookInfo(book: Book) {
        if (book.isLocalBook()) {
            loadChapterList(book)
        } else {
            BookRead.bookSource?.let { source ->
                WebBook.getBookInfo(viewModelScope, source, book, canReName = false)
                    .onSuccess {
                        loadChapterList(book)
                    }.onError {
                        BookRead.upMsg("详情页出错: ${it.localizedMessage}")
                    }
            }
        }
    }

    fun loadChapterList(book: Book) {
        if (book.isLocalBook()) {
            execute {
                LocalBook.getChapterList(book).let {
                    appDb.bookChapterDao.delByBook(book.bookUrl)
                    appDb.bookChapterDao.insert(*it.toTypedArray())
                    appDb.bookDao.update(book)
                    BookRead.chapterSize = it.size
                    BookRead.upMsg(null)
                    BookRead.loadContent(resetPageOffset = true)
                }
            }.onError {
                BookRead.upMsg("LoadTocError:${it.localizedMessage}")
            }
        } else {
            BookRead.bookSource?.let {
                WebBook.getChapterList(viewModelScope, it, book)
                    .onSuccess(IO) { cList ->
                        appDb.bookChapterDao.insert(*cList.toTypedArray())
                        appDb.bookDao.update(book)
                        BookRead.chapterSize = cList.size
                        BookRead.upMsg(null)
                        BookRead.loadContent(resetPageOffset = true)
                    }.onError {
                        BookRead.upMsg(context.getString(R.string.error_load_toc))
                    }
            }
        }
    }

    fun syncBookProgress(
        book: Book,
        syncBookProgress: Boolean = AppConfig.syncBookProgress,
        alertSync: ((progress: BookProgress) -> Unit)? = null
    ) {
        if (syncBookProgress)
            execute {
                AppWebDav.getBookProgress(book)
            }.onSuccess {
                it?.let { progress ->
                    if (progress.durChapterIndex < book.durChapterIndex ||
                        (progress.durChapterIndex == book.durChapterIndex && progress.durChapterPos < book.durChapterPos)
                    ) {
                        alertSync?.invoke(progress)
                    } else {
                        BookRead.setProgress(progress)
                    }
                }
            }
    }

    fun changeTo(source: BookSource, book: Book) {
        execute {
            BookRead.upMsg(context.getString(R.string.loading))
            if (book.tocUrl.isEmpty()) {
                WebBook.getBookInfoAwait(this, source, book)
            }
            val chapters = WebBook.getChapterListAwait(this, source, book)
            val oldBook = BookRead.book!!
            book.durChapterIndex = BookHelp.getDurChapter(
                oldBook.durChapterIndex,
                oldBook.totalChapterNum,
                oldBook.durChapterTitle,
                chapters
            )
            book.durChapterTitle = chapters[book.durChapterIndex].title
            oldBook.changeTo(book)
            appDb.bookChapterDao.insert(*chapters.toTypedArray())
            BookRead.resetData(book)
            BookRead.upMsg(null)
            BookRead.loadContent(resetPageOffset = true)
        }.timeout(60000)
            .onError {
                context.toastOnUi("换源失败\n${it.localizedMessage}")
                BookRead.upMsg(null)
            }.onFinally {
                postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
            }
    }

    private fun autoChangeSource(name: String, author: String) {
        if (!AppConfig.autoChangeSource) return
        execute {
            val sources = appDb.bookSourceDao.allTextEnabled
            WebBook.preciseSearch(this, sources, name, author)?.let {
                it.second.upInfoFromOld(BookRead.book)
                changeTo(it.first, it.second)
            } ?: throw NoStackTraceException("自动换源失败")
        }.onStart {
            BookRead.upMsg(context.getString(R.string.source_auto_changing))
        }.onError {
            context.toastOnUi(it.msg)
        }.onFinally {
            BookRead.upMsg(null)
        }
    }

    fun openChapter(index: Int, durChapterPos: Int = 0, success: (() -> Unit)? = null) {
        BookRead.clearTextChapter()
        BookRead.callBack?.upContent()
        if (index != BookRead.durChapterIndex) {
            BookRead.durChapterIndex = index
            BookRead.durChapterPos = durChapterPos
        }
        BookRead.saveRead()
        BookRead.loadContent(resetPageOffset = true) {
            success?.invoke()
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            Book.delete(BookRead.book)
        }.onSuccess {
            success?.invoke()
        }
    }

    fun upBookSource(success: (() -> Unit)?) {
        execute {
            BookRead.book?.let { book ->
                BookRead.bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun refreshContent(book: Book) {
        execute {
            appDb.bookChapterDao.getChapter(book.bookUrl, BookRead.durChapterIndex)
                ?.let { chapter ->
                    BookHelp.delContent(book, chapter)
                    BookRead.loadContent(BookRead.durChapterIndex, resetPageOffset = false)
                }
        }
    }

    fun reverseContent(book: Book) {
        execute {
            appDb.bookChapterDao.getChapter(book.bookUrl, BookRead.durChapterIndex)
                ?.let { chapter ->
                    BookHelp.reverseContent(book, chapter)
                    BookRead.loadContent(BookRead.durChapterIndex, resetPageOffset = false)
                }
        }
    }

    /**
     * 内容搜索跳转
     */
    fun searchResultPositions(
        pages: List<TextPage>,
        indexWithinChapter: Int
    ): Array<Int> {
        // calculate search result's pageIndex
        var content = ""
        pages.map {
            content += it.text
        }
        var count = 1
        var index = content.indexOf(searchContentQuery)
        while (count != indexWithinChapter) {
            index = content.indexOf(searchContentQuery, index + 1)
            count += 1
        }
        val contentPosition = index
        var pageIndex = 0
        var length = pages[pageIndex].text.length
        while (length < contentPosition) {
            pageIndex += 1
            if (pageIndex > pages.size) {
                pageIndex = pages.size
                break
            }
            length += pages[pageIndex].text.length
        }

        // calculate search result's lineIndex
        val currentPage = pages[pageIndex]
        var lineIndex = 0
        length = length - currentPage.text.length + currentPage.textLines[lineIndex].text.length
        while (length < contentPosition) {
            lineIndex += 1
            if (lineIndex > currentPage.textLines.size) {
                lineIndex = currentPage.textLines.size
                break
            }
            length += currentPage.textLines[lineIndex].text.length
        }

        // charIndex
        val currentLine = currentPage.textLines[lineIndex]
        length -= currentLine.text.length
        val charIndex = contentPosition - length
        var addLine = 0
        var charIndex2 = 0
        // change line
        if ((charIndex + searchContentQuery.length) > currentLine.text.length) {
            addLine = 1
            charIndex2 = charIndex + searchContentQuery.length - currentLine.text.length - 1
        }
        // changePage
        if ((lineIndex + addLine + 1) > currentPage.textLines.size) {
            addLine = -1
            charIndex2 = charIndex + searchContentQuery.length - currentLine.text.length - 1
        }
        return arrayOf(pageIndex, lineIndex, charIndex, addLine, charIndex2)
    }

    /**
     * 替换规则变化
     */
    fun replaceRuleChanged() {
        execute {
            BookRead.book?.let {
                ContentProcessor.get(it.name, it.origin).upReplaceRules()
                BookRead.loadContent(resetPageOffset = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (BaseReadAloudService.pause) {
            ReadAloud.stop(context)
        }
    }

}