package io.legado.app.help

import android.content.Context
import android.content.Intent
import io.legado.app.App
import io.legado.app.constant.Action
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.HttpReadAloudService
import io.legado.app.service.TTSReadAloudService
import io.legado.app.utils.getPrefBoolean

object ReadAloud {
    var aloudClass: Class<*> = getReadAloudClass()

    fun getReadAloudClass(): Class<*> {
        return if (App.INSTANCE.getPrefBoolean("tts", true)) {
            TTSReadAloudService::class.java
        } else {
            HttpReadAloudService::class.java
        }
    }

    fun play(
        context: Context,
        title: String,
        subtitle: String,
        pageIndex: Int,
        dataKey: String,
        play: Boolean = true
    ) {
        val readAloudIntent = Intent(context, aloudClass)
        readAloudIntent.action = Action.play
        readAloudIntent.putExtra("title", title)
        readAloudIntent.putExtra("subtitle", subtitle)
        readAloudIntent.putExtra("pageIndex", pageIndex)
        readAloudIntent.putExtra("dataKey", dataKey)
        readAloudIntent.putExtra("play", play)
        context.startService(readAloudIntent)
    }

    fun pause(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = Action.pause
            context.startService(intent)
        }
    }

    fun resume(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = Action.resume
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = Action.stop
            context.startService(intent)
        }
    }

    fun prevParagraph(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = Action.prevParagraph
            context.startService(intent)
        }
    }

    fun nextParagraph(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = Action.nextParagraph
            context.startService(intent)
        }
    }

    fun upTtsSpeechRate(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = Action.upTtsSpeechRate
            context.startService(intent)
        }
    }

    fun setTimer(context: Context, minute: Int) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = Action.setTimer
            intent.putExtra("minute", minute)
            context.startService(intent)
        }
    }

}