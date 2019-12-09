package io.legado.app.help

import android.app.PendingIntent
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

    fun toInstallUnknown(context: Context) {
        try {
            val intent = Intent()
            intent.action = "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (ignored: Exception) {
            context.toast("无法打开设置")
        }
    }

    inline fun <reified T> servicePendingIntent(context: Context, action: String): PendingIntent? {
        return PendingIntent.getService(
            context,
            0,
            Intent(context, T::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    inline fun <reified T> activityPendingIntent(context: Context, action: String): PendingIntent? {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, T::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}