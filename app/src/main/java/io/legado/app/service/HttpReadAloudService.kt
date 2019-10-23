package io.legado.app.service

import android.app.PendingIntent
import android.media.MediaPlayer
import io.legado.app.constant.Bus
import io.legado.app.data.api.IHttpPostApi
import io.legado.app.help.FileHelp
import io.legado.app.help.IntentHelp
import io.legado.app.help.http.HttpHelper
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileInputStream

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

    private fun getAudioBody(): Map<String, String> {
        val spd = (getPrefInt("ttsSpeechRate", 25) + 5) / 5
        val per = getPrefString("ttsSpeechPer") ?: "0"
        return mapOf(
            Pair("idx", "1"),
            Pair("tex", contentList[nowSpeak]),
            Pair("cuid", "baidu_speech_demo "),
            Pair("cod", "2"),
            Pair("lan", "zh"),
            Pair("ctp", "1"),
            Pair("pdt", "1"),
            Pair("spd", spd.toString()),
            Pair("per", per),
            Pair("vol", "5"),
            Pair("pit", "5"),
            Pair("_res_tag_", "audio")
        )
    }

    override fun play() {
        if (contentList.isEmpty()) return
        launch(IO) {
            if (requestFocus()) {
                val bytes = HttpHelper.getByteRetrofit("http://tts.baidu.com")
                    .create(IHttpPostApi::class.java)
                    .postMapByte("http://tts.baidu.com/text2audio", getAudioBody(), mapOf())
                    .execute().body()
                if (bytes == null) {
                    withContext(Main) {
                        toast("访问失败")
                    }
                } else {
                    val file =
                        FileHelp.getFile(cacheDir.absolutePath + File.separator + "bdTts.mp3")
                    file.writeBytes(bytes)
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(FileInputStream(file).fd)
                    mediaPlayer.prepareAsync()
                }
            }
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
        if (pause) return
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
        return true
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

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return IntentHelp.servicePendingIntent<HttpReadAloudService>(this, actionStr)
    }
}