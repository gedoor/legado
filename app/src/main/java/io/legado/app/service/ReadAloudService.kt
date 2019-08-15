package io.legado.app.service

import android.content.Intent
import android.speech.tts.TextToSpeech
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.utils.toast
import kotlinx.coroutines.launch
import java.util.*

class ReadAloudService : BaseService(), TextToSpeech.OnInitListener {

    companion object {
        fun paly() {

        }

        fun pause() {

        }

        fun resume() {

        }

        fun stop() {

        }
    }

    private var textToSpeech: TextToSpeech? = null
    private var ttsIsSuccess: Boolean = false

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                "play" -> {

                }
                "pause" -> {

                }
                "resume" -> {

                }
                "stop" -> {

                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onInit(status: Int) {
        launch {
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    toast(R.string.tts_fix)
                    toTTSSetting()
                } else {
                    ttsIsSuccess = true
                }
            } else {
                toast(R.string.tts_init_failed)
            }
        }
    }

    private fun toTTSSetting() {
        //跳转到文字转语音设置界面

        try {
            val intent = Intent()
            intent.action = "com.android.settings.TTS_SETTINGS"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (ignored: Exception) {
            toast(R.string.tip_cannot_jump_setting_page)
        }
    }

}