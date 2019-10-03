package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import io.legado.app.constant.Bus
import io.legado.app.help.ActivityHelp
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.LogUtils
import io.legado.app.utils.postEvent


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */

class MediaButtonReceiver : BroadcastReceiver() {

    companion object {

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            val keyEventAction = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.action
            LogUtils.d("耳机按键", "$intentAction $keyEventAction")
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                if (keyEventAction == KeyEvent.ACTION_DOWN) {
                    readAloud(context)
                    return true
                }
            }
            return false
        }

        private fun readAloud(context: Context) {
            if (!ActivityHelp.isExist(ReadBookActivity::class.java)) {
                val intent = Intent(context, ReadBookActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("readAloud", true)
                context.startActivity(intent)
            } else {
                postEvent(Bus.READ_ALOUD_BUTTON, true)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

}
