package io.legado.app.service

import android.app.PendingIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.help.AppConfig
import io.legado.app.help.MediaHelp
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.model.ReadBook
import io.legado.app.utils.*
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

    override fun onDestroy() {
        super.onDestroy()
        clearTTS()
    }

    @Synchronized
    private fun initTts() {
        ttsInitFinish = false
        val engine = GSON.fromJsonObject<SelectItem<String>>(AppConfig.ttsEngine)?.value
        textToSpeech = if (engine.isNullOrBlank()) {
            TextToSpeech(this, this)
        } else {
            TextToSpeech(this, this, engine)
        }
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
            toastOnUi(R.string.tts_init_failed)
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

    override fun playStop() {
        textToSpeech?.stop()
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

        override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
            super.onAudioAvailable(utteranceId, audio)

        }

        override fun onError(s: String) {
            //nothing
        }

    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<TTSReadAloudService>(actionStr)
    }

}