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
import io.legado.app.help.book.update
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
    var durChapterPos = 0
    fun initData(intent: Intent, success: () -> Unit) {
        execute {
            videoUrl = intent.getStringExtra("videoUrl") ?: ""
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
            }?.also { b ->
                durChapterIndex = b.durChapterIndex
                durChapterPos = b.durChapterPos
                source = appDb.bookSourceDao.getBookSource(b.origin)?.also {
                    sourceKey = b.origin
                    sourceType = SourceType.book
                }
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

    fun saveRead(first: Boolean = false) {
        book?.let { book ->
            book.lastCheckCount = 0
            book.durChapterTime = System.currentTimeMillis()
            val chapterChanged = book.durChapterIndex != durChapterIndex
            book.durChapterIndex = durChapterIndex
            book.durChapterPos = durChapterPos
            if (first || chapterChanged) {
                chapter = toc?.getOrNull(durChapterIndex)?.also {
                    book.durChapterTitle = it.title
                }
            }
            book.update()
        }
    }

    fun upDurIndex(offset: Int): Boolean {
        val index = durChapterIndex + offset
        if (index < 0 || index >= (toc?.size ?: 0)) {
            context.toastOnUi("没有更多章节了")
            return false
        }
        durChapterIndex = index
        durChapterPos = 0
        return true
    }
}