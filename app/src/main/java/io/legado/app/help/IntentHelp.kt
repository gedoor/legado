package io.legado.app.help

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.legado.app.R
import org.jetbrains.anko.toast

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
            context.toast(R.string.tip_cannot_jump_setting_page)
        }
    }

    fun toInstallUnknown(context: Context) {
        kotlin.runCatching {
            val intent = Intent()
            intent.action = "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }.onFailure {
            context.toast("无法打开设置")
        }
    }

    inline fun <reified T> servicePendingIntent(
        context: Context,
        action: String,
        bundle: Bundle? = null
    ): PendingIntent? {
        val intent = Intent(context, T::class.java)
        intent.action = action
        bundle?.let {
            intent.putExtras(bundle)
        }
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    inline fun <reified T> activityPendingIntent(
        context: Context,
        action: String,
        bundle: Bundle? = null
    ): PendingIntent? {
        val intent = Intent(context, T::class.java)
        intent.action = action
        bundle?.let {
            intent.putExtras(bundle)
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}