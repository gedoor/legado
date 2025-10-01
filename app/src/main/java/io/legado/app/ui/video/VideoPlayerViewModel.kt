package io.legado.app.ui.video

import android.app.Application
import android.content.Intent
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.getBookSource
import io.legado.app.model.AudioPlay
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.toastOnUi

class VideoPlayerViewModel(application: Application) : BaseViewModel(application) {
    var videoUrl = ""
    var videoTitle = application.getString(R.string.video_play)
    var source : BaseSource? = null
    var book: Book? = null
    var chapter: BookChapter? = null
    var sourceKey : String? = null
    var type : String? = null
    var bookUrl : String? = null
    var isNew = true
    fun initData(intent: Intent, success: () -> Unit) {
        execute {
            videoUrl = intent.getStringExtra("videoUrl")?.takeIf { it.isNotBlank() }
                ?: throw NoStackTraceException("videoUrl is null or empty")
            isNew = intent.getBooleanExtra("isNew", true)
            intent.getStringExtra("videoTitle")?.let { videoTitle = it }
            sourceKey = intent.getStringExtra("sourceKey")?.also {
                type = intent.getStringExtra("type")
                source = when (type) {
                    "bookSource" -> appDb.bookSourceDao.getBookSource(it)
                    "rssSource" -> appDb.rssSourceDao.getByKey(it)
                    else -> null
                }
            }
            bookUrl = intent.getStringExtra("bookUrl")
            book = bookUrl?.let {
                appDb.bookDao.getBook(it) ?: appDb.searchBookDao.getSearchBook(it)?.toBook()
            }
            chapter = book?.let { appDb.bookChapterDao.getChapter(it.bookUrl, it.durChapterIndex) }
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
                source = when (type) {
                    "bookSource" -> appDb.bookSourceDao.getBookSource(it)
                    "rssSource" -> appDb.rssSourceDao.getByKey(it)
                    else -> null
                }
            }
        }
    }
}