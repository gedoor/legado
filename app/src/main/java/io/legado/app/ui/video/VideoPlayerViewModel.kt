package io.legado.app.ui.video

import android.app.Application
import android.content.Intent
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.SourceType
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.update
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.toastOnUi

class VideoPlayerViewModel(application: Application) : BaseViewModel(application) {
    var videoUrl = ""
    var videoTitle: String? = null
    var source: BaseSource? = null
    var book: Book? = null
    var toc: List<BookChapter>? = null
    var chapter: BookChapter? = null
    var sourceKey: String? = null
    var sourceType: Int? = null
    var bookUrl: String? = null
    var durChapterIndex = 0
    var isNew = true
    fun initData(intent: Intent, success: () -> Unit) {
        execute {
            videoUrl = intent.getStringExtra("videoUrl") ?: ""
            isNew = intent.getBooleanExtra("isNew", true)
            videoTitle =
                intent.getStringExtra("videoTitle") ?: context.getString(R.string.video_play)
            sourceKey = intent.getStringExtra("sourceKey")?.also {
                sourceType = intent.getIntExtra("sourceType", 0)
                source = when (sourceType) {
                    SourceType.book -> appDb.bookSourceDao.getBookSource(it)
                    SourceType.rss -> appDb.rssSourceDao.getByKey(it)
                    else -> null
                }
            }
            bookUrl = intent.getStringExtra("bookUrl")
            book = bookUrl?.let {
                toc = appDb.bookChapterDao.getChapterList(it)
                appDb.bookDao.getBook(it) ?: appDb.searchBookDao.getSearchBook(it)?.toBook()
            }?.also {
                durChapterIndex = it.durChapterIndex
                source = appDb.bookSourceDao.getBookSource(it.origin)
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
    }

    fun upSource() {
        execute {
            sourceKey?.also {
                source = when (sourceType) {
                    SourceType.book -> appDb.bookSourceDao.getBookSource(it)
                    SourceType.rss -> appDb.rssSourceDao.getByKey(it)
                    else -> null
                }
            }
        }
    }

    fun upVideoUrl(success: (() -> Unit)? = null) {
        execute {
            if (source is BookSource) {
                book?.let { b -> chapter?.let { c -> videoUrl = WebBook.getContentAwait(source as BookSource, b, c) } }
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun saveRead(first: Boolean = false, success: (() -> Unit)? = null) {
        execute {
            book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                val chapterChanged = book.durChapterIndex != durChapterIndex
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = 0
                if (first || chapterChanged) {
                    chapter = toc?.getOrNull(durChapterIndex)?.also {
                        book.durChapterTitle = it.getDisplayTitle(
                            ContentProcessor.get(book.name, book.origin).getTitleReplaceRules(),
                            book.getUseReplaceRule()
                        )
                    }
                }
                book.update()
            }
        }.onSuccess {
            success?.invoke()
        }
    }

    fun upDurIndex (offset: Int): Boolean {
        val index = durChapterIndex + offset
        if (index < 0 || index >= (toc?.size ?: 0)) {
            context.toastOnUi("没有更多章节了")
            return false
        }
        durChapterIndex = index
        return true
    }
}