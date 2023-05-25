package io.legado.app.help

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.utils.buildMainHandler
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

class TTS {

    private val handler by lazy { buildMainHandler() }

    private val tag = "legado_tts"

    private val clearTtsRunnable = Runnable { clearTts() }

    private var speakStateListener: SpeakStateListener? = null

    private var textToSpeech: TextToSpeech? = null

    private var text: String? = null

    private var onInit = false

    private val initListener by lazy {
        InitListener()
    }

    private val utteranceListener by lazy {
        TTSUtteranceListener()
    }

    val isSpeaking: Boolean
        get() {
            return textToSpeech?.isSpeaking ?: false
        }

    @Suppress("unused")
    fun setSpeakStateListener(speakStateListener: SpeakStateListener) {
        this.speakStateListener = speakStateListener
    }

    @Suppress("unused")
    fun removeSpeakStateListener() {
        speakStateListener = null
    }

    @Synchronized
    fun speak(text: String) {
        handler.removeCallbacks(clearTtsRunnable)
        this.text = text
        if (onInit) {
            return
        }
        if (textToSpeech == null) {
            onInit = true
            textToSpeech = TextToSpeech(appCtx, initListener)
        } else {
            addTextToSpeakList()
        }
    }

    fun stop() {
        textToSpeech?.stop()
    }

    @Synchronized
    fun clearTts() {
        textToSpeech?.let { tts ->
            tts.stop()
            tts.shutdown()
        }
        textToSpeech = null
    }

    private fun addTextToSpeakList() {
        val tts = textToSpeech ?: return
        kotlin.runCatching {
            var result = tts.speak("", TextToSpeech.QUEUE_FLUSH, null, null)
            if (result == TextToSpeech.ERROR) {
                clearTts()
                textToSpeech = TextToSpeech(appCtx, initListener)
                return
            }
            text?.splitNotBlank("\n")?.forEachIndexed { i, s ->
                result = tts.speak(s, TextToSpeech.QUEUE_ADD, null, tag + i)
                if (result == TextToSpeech.ERROR) {
                    AppLog.put("tts朗读出错:$text")
                }
            }
        }.onFailure {
            AppLog.put("tts朗读出错", it)
            appCtx.toastOnUi(it.localizedMessage)
        }
    }

    /**
     * 初始化监听
     */
    private inner class InitListener : TextToSpeech.OnInitListener {

        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setOnUtteranceProgressListener(utteranceListener)
                addTextToSpeakList()
            } else {
                appCtx.toastOnUi(R.string.tts_init_failed)
            }
            onInit = false
        }

    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            //开始朗读取消释放资源任务
            handler.removeCallbacks(clearTtsRunnable)
            speakStateListener?.onStart()
        }

        override fun onDone(utteranceId: String?) {
            //一分钟没有朗读释放资源
            handler.postDelayed(clearTtsRunnable, 60000L)
            speakStateListener?.onDone()
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            //Deprecated
        }

    }

    interface SpeakStateListener {
        fun onStart()
        fun onDone()
    }
}