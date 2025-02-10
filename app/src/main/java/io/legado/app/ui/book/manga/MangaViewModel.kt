package io.legado.app.ui.book.manga

import android.app.Application
import android.content.Intent
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isLocalModified
import io.legado.app.help.book.removeType
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.ReadMange
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.mapParallelSafe
import io.legado.app.utils.postEvent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take

class MangaViewModel(application: Application) : BaseViewModel(application) {

    private var changeSourceCoroutine: Coroutine<*>? = null

    /**
     * 初始化
     */
    fun initData(intent: Intent, success: (() -> Unit)? = null) {
        execute {
            ReadMange.inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            ReadMange.tocChanged = intent.getBooleanExtra("tocChanged", false)
            val bookUrl = intent.getStringExtra("bookUrl")
            val book = when {
                bookUrl.isNullOrEmpty() -> appDb.bookDao.lastReadBook
                else -> appDb.bookDao.getBook(bookUrl)
            } ?: ReadMange.book
            when {
                book != null -> initMange(book)
                else -> context.getString(R.string.no_book)//没有找到书
            }
        }.onSuccess {
            success?.invoke()
        }.onError {
            val msg = "初始化数据失败\n${it.localizedMessage}"
            AppLog.put(msg, it)
        }.onFinally {
            ReadMange.saveRead()
        }
    }

    private suspend fun initMange(book: Book) {
        val isSameBook = ReadMange.book?.bookUrl == book.bookUrl
        if (isSameBook) {
            ReadMange.upData(book)
        } else {
            ReadMange.resetData(book)
        }
        if (!book.isLocal && book.tocUrl.isEmpty() && !loadBookInfo(book)) {
            return
        }

        if (book.isLocal && !checkLocalBookFileExist(book)) {
            return
        }

        if ((ReadMange.durChapterPageCount == 0 || book.isLocalModified()) && !loadChapterListAwait(
                book
            )
        ) {
            return
        }
        ensureChapterExist()

        //开始加载内容
        ReadMange.loadContent()

        //自动换源
        if (!book.isLocal && ReadMange.bookSource == null) {
            autoChangeSource(book.name, book.author)
            return
        }
    }

    private suspend fun loadChapterListAwait(book: Book): Boolean {
        ReadMange.bookSource?.let {
            val oldBook = book.copy()
            WebBook.getChapterListAwait(it, book, true).onSuccess { cList ->
                if (oldBook.bookUrl == book.bookUrl) {
                    appDb.bookDao.update(book)
                } else {
                    appDb.bookDao.insert(book)
                    BookHelp.updateCacheFolder(oldBook, book)
                }
                appDb.bookChapterDao.delByBook(oldBook.bookUrl)
                appDb.bookChapterDao.insert(*cList.toTypedArray())
                ReadMange.durChapterPageCount = cList.size
                ReadMange.simulatedChapterSize = book.simulatedTotalChapterNum()
                return true
            }.onFailure {
                //加载章节出错
                ReadMange.mCallback?.loadFail("加载目录失败")
                return false
            }
        }

        return true

    }

    /**
     * 加载详情页
     */
    private suspend fun loadBookInfo(book: Book): Boolean {
        val source = ReadMange.bookSource ?: return true
        try {
            WebBook.getBookInfoAwait(source, book, canReName = false)
            return true
        } catch (e: Throwable) {
            //  加载详情页失败
//            ReadBook.upMsg("详情页出错: ${e.localizedMessage}")
            return false
        }
    }

    private fun ensureChapterExist() {
        if (ReadMange.simulatedChapterSize > 0 && ReadMange.durChapterPagePos > ReadMange.simulatedChapterSize - 1) {
            ReadMange.durChapterPagePos = ReadMange.simulatedChapterSize - 1
        }
    }

    /**
     * 自动换源
     */
    private fun autoChangeSource(name: String, author: String) {
        if (!AppConfig.autoChangeSource) return
        execute {
            val sources = appDb.bookSourceDao.allTextEnabledPart
            flow {
                for (source in sources) {
                    source.getBookSource()?.let {
                        emit(it)
                    }
                }
            }.onStart {
                // 自动换源

            }.mapParallelSafe(AppConfig.threadCount) { source ->
                val book = WebBook.preciseSearchAwait(this, source, name, author).getOrThrow()
                if (book.tocUrl.isEmpty()) {
                    WebBook.getBookInfoAwait(source, book)
                }
                val toc = WebBook.getChapterListAwait(source, book).getOrThrow()
                val chapter = toc.getOrElse(book.durChapterIndex) {
                    toc.last()
                }
                val nextChapter = toc.getOrElse(chapter.index) {
                    toc.first()
                }
                WebBook.getContentAwait(
                    bookSource = source,
                    book = book,
                    bookChapter = chapter,
                    nextChapterUrl = nextChapter.url
                )
                book to toc
            }.take(1).onEach { (book, toc) ->
                changeTo(book, toc)
            }.onEmpty {
                throw NoStackTraceException("没有合适书源")
            }.onCompletion {
                // 换源完成
            }.catch {
                AppLog.put("自动换源失败\n${it.localizedMessage}", it)
                context.toastOnUi("自动换源失败\n${it.localizedMessage}")
            }.collect()
        }
    }

    /**
     * 换源
     */
    fun changeTo(book: Book, toc: List<BookChapter>) {
        changeSourceCoroutine?.cancel()
        changeSourceCoroutine = execute {
            //换源中
            ReadMange.book?.migrateTo(book, toc)
            book.removeType(BookType.updateError)
            ReadMange.book?.delete()
            appDb.bookDao.insert(book)
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            ReadMange.resetData(book)
            toc.find { it.title.contains(ReadMange.chapterTitle) }?.run {
                ReadMange.loadContent(index)
            } ?: ReadMange.loadContent()
        }.onError {
            AppLog.put("换源失败\n$it", it, true)
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
        }
    }

    private fun checkLocalBookFileExist(book: Book): Boolean {
        try {
            LocalBook.getBookInputStream(book)
            return true
        } catch (e: Throwable) {
            return false
        }
    }

    fun openChapter(index: Int, durChapterPos: Int = 0) {
        if (index < ReadMange.durChapterPageCount) {
            ReadMange.chapterChanged = true
            ReadMange.durChapterPagePos = index
            ReadMange.durChapterPos = durChapterPos
            ReadMange.saveRead()
            ReadMange.loadContent(index)
        }
    }

    override fun onCleared() {
        super.onCleared()
        changeSourceCoroutine?.cancel()
    }
}