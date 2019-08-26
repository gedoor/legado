package io.legado.app.help

import android.content.Context
import android.content.Intent
import io.legado.app.R
import org.jetbrains.anko.toast

object IntentHelp {


    fun toTTSSetting(context: Context) {
        //跳转到文字转语音设置界面
        try {
            val intent = Intent()
            intent.action = "com.android.settings.TTS_SETTINGS"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (ignored: Exception) {
            context.toast(R.string.tip_cannot_jump_setting_page)
        }
    }
}