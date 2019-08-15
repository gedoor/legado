package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */

class MediaButtonIntentReceiver : BroadcastReceiver() {

    companion object {
        val TAG = MediaButtonIntentReceiver::class.java.simpleName

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false

                val keycode = event.keyCode
                val action = event.action

                val command: String? = null
                when (keycode) {
                    KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    }
                    else -> {
                    }
                }//                    command = ReadAloudService.ActionMediaButton;
                if (command != null) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        readAloud(context, command)
                        return true
                    }
                }
            }
            return false
        }

        private fun readAloud(context: Context, command: String?) {

        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

}
