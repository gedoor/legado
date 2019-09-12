package io.legado.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import io.legado.app.constant.Bus
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast

class HttpReadAloudService : BaseReadAloudService(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private var mediaPlayer = MediaPlayer()

    override fun onCreate() {
        super.onCreate()
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    private fun getAudioPath(): String {
        val audioText = contentList[nowSpeak]
        val spd = (getPrefInt("ttsSpeechRate", 25) + 5) / 5
        val per = getPrefString("ttsSpeechPer") ?: "0"
        return "http://tts.baidu.com/text2audio?idx=1&tex=$audioText&cuid=baidu_speech_demo&cod=2&lan=zh&ctp=1&pdt=1&spd=$spd&per=$per&vol=5&pit=5&_res_tag_=audio"
    }

    override fun play() {
        if (contentList.isEmpty()) return
        if (requestFocus()) {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(getAudioPath())
            mediaPlayer.prepareAsync()
        }
    }

    override fun pauseReadAloud(pause: Boolean) {
        super.pauseReadAloud(pause)
        mediaPlayer.pause()
    }

    override fun resumeReadAloud() {
        super.resumeReadAloud()
        mediaPlayer.start()
    }

    override fun upSpeechRate(reset: Boolean) {
        mediaPlayer.stop()
        play()
    }

    override fun prevP() {
        if (nowSpeak > 0) {
            mediaPlayer.stop()
            nowSpeak--
            readAloudNumber -= contentList[nowSpeak].length.minus(1)
            play()
        }
    }

    override fun nextP() {
        if (nowSpeak < contentList.size - 1) {
            mediaPlayer.stop()
            readAloudNumber += contentList[nowSpeak].length.plus(1)
            nowSpeak++
            play()
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        super.play()
        mp?.start()
        textChapter?.let {
            if (readAloudNumber + 1 > it.getReadLength(pageIndex + 1)) {
                pageIndex++
                postEvent(Bus.TTS_TURN_PAGE, 1)
            }
        }
        postEvent(Bus.TTS_START, readAloudNumber + 1)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        launch { toast("播放出错") }
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        readAloudNumber += contentList[nowSpeak].length + 1
        if (nowSpeak < contentList.size) {
            nowSpeak++
            play()
        } else {
            postEvent(Bus.TTS_TURN_PAGE, 2)
        }
    }

    override fun aloudServicePendingIntent(context: Context, actionStr: String): PendingIntent {
        val intent = Intent(context, HttpReadAloudService::class.java)
        intent.action = actionStr
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}