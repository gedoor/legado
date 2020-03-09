package io.legado.app.service

import android.app.PendingIntent
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.help.AppConfig
import io.legado.app.help.IntentHelp
import io.legado.app.help.MediaHelp
import io.legado.app.service.help.ReadBook
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.postEvent
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*

class TTSReadAloudService : BaseReadAloudService(), TextToSpeech.OnInitListener {

    companion object {
        var textToSpeech: TextToSpeech? = null

        fun clearTTS() {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }

    private var ttsIsSuccess: Boolean = false

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
        upSpeechRate()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTTS()
    }

    override fun onInit(status: Int) {
        launch {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.CHINA
                textToSpeech?.setOnUtteranceProgressListener(TTSUtteranceListener())
                ttsIsSuccess = true
                play()
            } else {
                toast(R.string.tts_init_failed)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun play() {
        if (contentList.isEmpty() || !ttsIsSuccess) {
            return
        }
        if (requestFocus()) {
            MediaHelp.playSilentSound(this)
            super.play()
            for (i in nowSpeak until contentList.size) {
                if (i == 0) {
                    speak(contentList[i], TextToSpeech.QUEUE_FLUSH, AppConst.APP_TAG + i)
                } else {
                    speak(contentList[i], TextToSpeech.QUEUE_ADD, AppConst.APP_TAG + i)
                }
            }
        }
    }

    private fun speak(content: String, queueMode: Int, utteranceId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(content, queueMode, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            textToSpeech?.speak(
                content,
                queueMode,
                hashMapOf(Pair(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId))
            )
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        if (this.getPrefBoolean("ttsFollowSys", true)) {
            if (reset) {
                clearTTS()
                textToSpeech = TextToSpeech(this, this)
            }
        } else {
            textToSpeech?.setSpeechRate((AppConfig.ttsSpeechRate + 5) / 10f)
        }
    }

    /**
     * 上一段
     */
    override fun prevP() {
        if (nowSpeak > 0) {
            textToSpeech?.stop()
            nowSpeak--
            readAloudNumber -= contentList[nowSpeak].length.minus(1)
            play()
        }
    }

    /**
     * 下一段
     */
    override fun nextP() {
        if (nowSpeak < contentList.size - 1) {
            textToSpeech?.stop()
            readAloudNumber += contentList[nowSpeak].length.plus(1)
            nowSpeak++
            play()
        }
    }

    /**
     * 暂停朗读
     */
    override fun pauseReadAloud(pause: Boolean) {
        super.pauseReadAloud(pause)
        textToSpeech?.stop()
    }

    /**
     * 恢复朗读
     */
    override fun resumeReadAloud() {
        super.resumeReadAloud()
        play()
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        override fun onStart(s: String) {
            textChapter?.let {
                if (readAloudNumber + 1 > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                }
            }
            postEvent(EventBus.TTS_PROGRESS, readAloudNumber + 1)
        }

        override fun onDone(s: String) {
            readAloudNumber += contentList[nowSpeak].length + 1
            nowSpeak++
            if (nowSpeak >= contentList.size) {
                nextChapter()
            }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            super.onRangeStart(utteranceId, start, end, frame)
            textChapter?.let {
                if (readAloudNumber + start > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                    postEvent(EventBus.TTS_PROGRESS, readAloudNumber + start)
                }
            }
        }

        override fun onError(s: String) {
            pauseReadAloud(true)
        }

    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return IntentHelp.servicePendingIntent<TTSReadAloudService>(this, actionStr)
    }

}