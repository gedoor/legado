package io.legado.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import io.legado.app.App
import io.legado.app.constant.Bus
import io.legado.app.data.entities.Book
import io.legado.app.help.ActivityHelp
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */

class MediaButtonReceiver : BroadcastReceiver() {

    companion object {

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            val keyEventAction = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                if (keyEventAction == KeyEvent.ACTION_DOWN) {
                    readAloud(context)
                    return true
                }
            }
            return false
        }

        private fun readAloud(context: Context) {
            when {
                ActivityHelp.isExist(AudioPlayActivity::class.java) ->
                    postEvent(Bus.AUDIO_PLAY_BUTTON, true)
                ActivityHelp.isExist(ReadBookActivity::class.java) ->
                    postEvent(Bus.READ_ALOUD_BUTTON, true)
                else -> {
                    GlobalScope.launch(Main) {
                        val lastBook: Book? = withContext(IO) {
                            App.db.bookDao().lastReadBook
                        }
                        lastBook?.let {
                            val intent = Intent(context, ReadBookActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.putExtra("readAloud", true)
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

}
