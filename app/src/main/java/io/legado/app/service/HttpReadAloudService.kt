package io.legado.app.service

import android.media.MediaPlayer
import io.legado.app.constant.Bus
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
        return "http://tts.baidu.com/text2audio?idx=1&tex=$audioText&cuid=baidu_speech_demo&cod=2&lan=zh&ctp=1&pdt=1&spd=4&per=6&vol=5&pit=5&_res_tag_=audio"
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
        mp?.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        launch { toast("播放出错") }
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (nowSpeak < contentList.size) {
            nowSpeak++
            play()
        } else {
            postEvent(Bus.TTS_TURN_PAGE, 2)
        }
    }
}