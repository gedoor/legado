package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.constant.Action
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.model.WebBook
import io.legado.app.service.AudioPlayService

object AudioPlay {
    var titleData = MutableLiveData<String>()
    var coverData = MutableLiveData<String>()
    var status = Status.STOP
    var book: Book? = null
    var inBookshelf = false
    var chapterSize = 0
    var durChapterIndex = 0
    var durPageIndex = 0
    var webBook: WebBook? = null
    val loadingChapters = arrayListOf<Int>()

    fun play(context: Context) {
        val intent = Intent(context, AudioPlayService::class.java)
        intent.action = Action.play
        context.startService(intent)
    }

    fun pause(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.pause
            context.startService(intent)
        }
    }

    fun resume(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.resume
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.stop
            context.startService(intent)
        }
    }

    fun adjustSpeed(context: Context, adjust: Float) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.adjustSpeed
            intent.putExtra("adjust", adjust)
            context.startService(intent)
        }
    }

    fun adjustProgress(context: Context, position: Int) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.adjustProgress
            intent.putExtra("position", position)
            context.startService(intent)
        }
    }

    fun prev(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.prev
            context.startService(intent)
        }
    }

    fun next(context: Context) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.next
            context.startService(intent)
        }
    }

    fun moveTo(context: Context, index: Int) {
        if (AudioPlayService.isRun) {
            val intent = Intent(context, AudioPlayService::class.java)
            intent.action = Action.moveTo
            intent.putExtra("index", index)
            context.startService(intent)
        }
    }
}