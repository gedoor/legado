package io.legado.app.help

import android.content.Context
import android.content.Intent
import io.legado.app.R
import io.legado.app.utils.toastOnUi

@Suppress("unused")
object IntentHelp {


    fun toTTSSetting(context: Context) {
        //跳转到文字转语音设置界面
        kotlin.runCatching {
            val intent = Intent()
            intent.action = "com.android.settings.TTS_SETTINGS"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }.onFailure {
            context.toastOnUi(R.string.tip_cannot_jump_setting_page)
        }
    }

    fun toInstallUnknown(context: Context) {
        kotlin.runCatching {
            val intent = Intent()
            intent.action = "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }.onFailure {
            context.toastOnUi("无法打开设置")
        }
    }

}