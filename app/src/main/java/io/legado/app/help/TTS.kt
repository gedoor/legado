package io.legado.app.help

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

class TTS {

    private val tag = "legado_tts"

    private var textToSpeech: TextToSpeech? = null

    private var text: String? = null

    private val initListener by lazy {
        InitListener()
    }

    private val utteranceListener by lazy {
        TTSUtteranceListener()
    }

    fun speak(text: String) {
        this.text = text
        if (textToSpeech == null) {

        } else {

        }
    }

    private fun initTts() {
        textToSpeech == TextToSpeech(appCtx, initListener)
    }

    private fun clearTts() {
        textToSpeech?.let { tts ->
            tts.stop()
            tts.shutdown()
        }
        textToSpeech = null
    }

    private fun addTextToSpeakList() {
        textToSpeech?.let { tts ->
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
    }

    private inner class InitListener : TextToSpeech.OnInitListener {

        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setOnUtteranceProgressListener(utteranceListener)
            } else {
                appCtx.toastOnUi(R.string.tts_init_failed)
            }
        }

    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            TODO("Not yet implemented")
        }

        override fun onDone(utteranceId: String?) {
            TODO("Not yet implemented")
        }

        override fun onError(utteranceId: String?) {
            TODO("Not yet implemented")
        }

    }
}