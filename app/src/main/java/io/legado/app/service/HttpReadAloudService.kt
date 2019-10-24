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
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.net.URLEncoder

class HttpReadAloudService : BaseReadAloudService(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private val mediaPlayer = MediaPlayer()
    private lateinit var ttsFolder: String
    private var playingIndex = -1

    override fun onCreate() {
        super.onCreate()
        ttsFolder = cacheDir.absolutePath + File.separator + "bdTts"
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    private fun getAudioBody(content: String): Map<String, String> {
        return mapOf(
            Pair("tex", URLEncoder.encode(URLEncoder.encode(content, "UTF-8"), "UTF-8")),
            Pair("spd", ((getPrefInt("ttsSpeechRate", 25) + 5) / 5).toString()),
            Pair("per", getPrefString("ttsSpeechPer") ?: "0"),
            Pair("cuid", "baidu_speech_demo"),
            Pair("idx", "1"),
            Pair("cod", "2"),
            Pair("lan", "zh"),
            Pair("ctp", "1"),
            Pair("pdt", "1"),
            Pair("vol", "5"),
            Pair("pit", "5"),
            Pair("_res_tag_", "audio")
        )
    }

    override fun play() {
        if (contentList.isEmpty()) return
        if (nowSpeak == 0) {
            downloadAudio()
        } else {
            val file = getSpeakFile(nowSpeak)
            if (file.exists()) {
                playAudio(FileInputStream(file).fd)
            }
        }
    }

    private fun downloadAudio() {
        launch(IO) {
            FileHelp.deleteFile(ttsFolder)
            for (index in 0 until contentList.size) {
                val bytes = HttpHelper.getByteRetrofit("http://tts.baidu.com")
                    .create(IHttpPostApi::class.java)
                    .postMapByte(
                        "http://tts.baidu.com/text2audio",
                        getAudioBody(contentList[index]), mapOf()
                    )
                    .execute().body()
                if (bytes == null) {
                    toast("访问失败")
                } else {
                    val file = getSpeakFile(index)
                    file.writeBytes(bytes)
                    if (index == nowSpeak) {
                        playAudio(FileInputStream(file).fd)
                    }
                }
            }
        }
    }

    @Synchronized
    private fun playAudio(fd: FileDescriptor) {
        if (playingIndex != nowSpeak) {
            playingIndex = nowSpeak
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(fd)
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getSpeakFile(index: Int = nowSpeak): File {
        return FileHelp.getFile("${ttsFolder}${File.separator}${index}.mp3")
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
        if (nowSpeak < contentList.lastIndex) {
            nowSpeak++
            play()
        } else {
            playingIndex = -1
            postEvent(Bus.TTS_TURN_PAGE, 2)
        }
    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return IntentHelp.servicePendingIntent<HttpReadAloudService>(this, actionStr)
    }
}