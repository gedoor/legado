package io.legado.app.service

import android.app.PendingIntent
import android.content.Intent
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

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitFinish = false
    private val ttsUtteranceListener = TTSUtteranceListener()

    override fun onCreate() {
        super.onCreate()
        initTts()
        upSpeechRate()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        clearTTS()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTTS()
    }

    @Synchronized
    private fun initTts() {
        ttsInitFinish = false
        textToSpeech = TextToSpeech(this, this)
    }

    @Synchronized
    fun clearTTS() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsInitFinish = false
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let {
                it.setOnUtteranceProgressListener(ttsUtteranceListener)
                it.language = Locale.CHINA
                ttsInitFinish = true
                play()
            }
        } else {
            launch {
                toast(R.string.tts_init_failed)
            }
        }
    }

    @Synchronized
    override fun play() {
        if (contentList.isNotEmpty() && ttsInitFinish && requestFocus()) {
            super.play()
            execute {
                MediaHelp.playSilentSound(this@TTSReadAloudService)
            }.onFinally {
                textToSpeech?.let {
                    it.speak("", TextToSpeech.QUEUE_FLUSH, null, null)
                    for (i in nowSpeak until contentList.size) {
                        it.speak(contentList[i], TextToSpeech.QUEUE_ADD, null, AppConst.APP_TAG + i)
                    }
                }
            }
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        if (this.getPrefBoolean("ttsFollowSys", true)) {
            if (reset) {
                clearTTS()
                initTts()
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
                postEvent(EventBus.TTS_PROGRESS, readAloudNumber + 1)
            }
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
            //nothing
        }

    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return IntentHelp.servicePendingIntent<TTSReadAloudService>(this, actionStr)
    }

}