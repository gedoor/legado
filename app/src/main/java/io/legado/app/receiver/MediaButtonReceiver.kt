package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.help.AppConfig
import io.legado.app.help.LifecycleHelp
import io.legado.app.service.AudioPlayService
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.help.AudioPlay
import io.legado.app.service.help.ReadAloud
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.audio.AudioPlayActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.postEvent
import splitties.init.appCtx


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */
class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    companion object {

        val handler = object : Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        ReadAloud.prevParagraph(appCtx)
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        ReadAloud.nextParagraph(appCtx)
                    }
                }
            }

        }

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    ?: return false
                val keycode: Int = keyEvent.keyCode
                val action: Int = keyEvent.action
                if (action == KeyEvent.ACTION_DOWN) {
                    when (keycode) {
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (handler.hasMessages(KeyEvent.KEYCODE_MEDIA_PREVIOUS)) {
                                handler.removeMessages(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                                ReadBook.moveToPrevChapter(true)
                            } else {
                                handler.sendEmptyMessageDelayed(
                                    KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                                    500
                                )
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (handler.hasMessages(KeyEvent.KEYCODE_MEDIA_NEXT)) {
                                handler.removeMessages(KeyEvent.KEYCODE_MEDIA_NEXT)
                                ReadBook.moveToNextChapter(true)
                            } else {
                                handler.sendEmptyMessageDelayed(KeyEvent.KEYCODE_MEDIA_NEXT, 500)
                            }
                        }
                        else -> readAloud(context)
                    }
                }
            }
            return true
        }

        fun readAloud(context: Context, isMediaKey: Boolean = true) {
            when {
                BaseReadAloudService.isRun -> if (BaseReadAloudService.isPlay()) {
                    ReadAloud.pause(context)
                    AudioPlay.pause(context)
                } else {
                    ReadAloud.resume(context)
                    AudioPlay.resume(context)
                }
                AudioPlayService.isRun -> if (AudioPlayService.pause) {
                    AudioPlay.resume(context)
                } else {
                    AudioPlay.pause(context)
                }
                LifecycleHelp.isExistActivity(ReadBookActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)
                LifecycleHelp.isExistActivity(AudioPlayActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)
                else -> if (AppConfig.mediaButtonOnExit || !isMediaKey) {
                    appDb.bookDao.lastReadBook?.let {
                        if (!LifecycleHelp.isExistActivity(MainActivity::class.java)) {
                            Intent(context, MainActivity::class.java).let {
                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(it)
                            }
                        }
                        Intent(context, ReadBookActivity::class.java).let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            it.putExtra("readAloud", true)
                            context.startActivity(it)
                        }
                    }
                }
            }
        }
    }

}
