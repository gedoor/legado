package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import io.legado.app.constant.Bus
import io.legado.app.help.ActivityHelp
import io.legado.app.ui.readbook.ReadBookActivity
import io.legado.app.utils.postEvent
import org.jetbrains.anko.startActivity


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */

class MediaButtonReceiver : BroadcastReceiver() {

    companion object {
        val TAG = MediaButtonReceiver::class.java.simpleName

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false

                val keycode = event.keyCode
                val action = event.action

                var command = false
                when (keycode) {
                    KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        command = true
                    }
                }
                if (command) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        readAloud(context)
                        return true
                    }
                }
            }
            return false
        }

        private fun readAloud(context: Context) {
            ActivityHelp.isExist(ReadBookActivity::class.java).let {
                if (!it) {
                    context.startActivity<ReadBookActivity>()
                }
            }
            postEvent(Bus.READ_ALOUD, "")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

}
