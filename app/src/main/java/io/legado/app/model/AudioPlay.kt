package io.legado.app.model

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.Status
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ContentProcessor
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.service.AudioPlayService
import io.legado.app.utils.postEvent
import io.legado.app.utils.startService
import splitties.init.appCtx

object AudioPlay {
    var titleData = MutableLiveData<String>()
    var coverData = MutableLiveData<String>()
    var status = Status.STOP
    var book: Book? = null
    var durChapter: BookChapter? = null
    var inBookshelf = false
    var bookSource: BookSource? = null
    val loadingChapters = arrayListOf<Int>()

    fun headers(hasLoginHeader: Boolean): Map<String, String>? {
        return bookSource?.getHeaderMap(hasLoginHeader)
    }

    /**
     * 播放当前章节
     */
    fun play(context: Context) {
        book?.let {
            if (durChapter == null) {
                upDurChapter(it)
            }
            durChapter?.let {
                context.startService<AudioPlayService> {
                    action = IntentAction.play
                }
            }
        }
    }

    /**
     * 更新当前章节
     */
    fun upDurChapter(book: Book) {
        durChapter = appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)
        postEvent(EventBus.AUDIO_SUB_TITLE, durChapter?.title ?: "")
        postEvent(EventBus.AUDIO_SIZE, durChapter?.end?.toInt() ?: 0)
        postEvent(EventBus.AUDIO_PROGRESS, book.durChapterPos)
    }

    fun pause(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.pause
            }
        }
    }

    fun resume(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.resume
            }
        }
    }

    fun stop(context: Context) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.stop
            }
        }
    }

    fun adjustSpeed(context: Context, adjust: Float) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.adjustSpeed
                putExtra("adjust", adjust)
            }
        }
    }

    fun adjustProgress(context: Context, position: Int) {
        if (AudioPlayService.isRun) {
            context.startService<AudioPlayService> {
                action = IntentAction.adjustProgress
                putExtra("position", position)
            }
        }
    }

    fun skipTo(context: Context, index: Int) {
        Coroutine.async {
            book?.let { book ->
                book.durChapterIndex = index
                book.durChapterPos = 0
                durChapter = null
                saveRead()
                play(context)
            }
        }
    }

    fun prev(context: Context) {
        Coroutine.async {
            book?.let { book ->
                if (book.durChapterIndex <= 0) {
                    return@let
                }
                book.durChapterIndex = book.durChapterIndex - 1
                book.durChapterPos = 0
                durChapter = null
                saveRead()
                play(context)
            }
        }
    }

    fun next(context: Context) {
        book?.let { book ->
            if (book.durChapterIndex >= book.totalChapterNum) {
                return@let
            }
            book.durChapterIndex = book.durChapterIndex + 1
            book.durChapterPos = 0
            durChapter = null
            saveRead()
            play(context)
        }
    }

    fun setTimer(minute: Int) {
        val intent = Intent(appCtx, AudioPlayService::class.java)
        intent.action = IntentAction.setTimer
        intent.putExtra("minute", minute)
        appCtx.startService(intent)
    }

    fun saveRead() {
        book?.let { book ->
            book.lastCheckCount = 0
            book.durChapterTime = System.currentTimeMillis()
            Coroutine.async {
                appDb.bookChapterDao.getChapter(book.bookUrl, book.durChapterIndex)?.let {
                    book.durChapterTitle = it.getDisplayTitle(
                        ContentProcessor.get(book.name, book.origin).getTitleReplaceRules()
                    )
                }
                book.save()
            }
        }
    }

    /**
     * 保存章节长度
     */
    fun saveDurChapter(audioSize: Long) {
        Coroutine.async {
            durChapter?.let {
                it.end = audioSize
                appDb.bookChapterDao.upDate(it)
            }
        }
    }
}