package io.legado.app.help

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.legado.app.service.TTSReadAloudService
import io.legado.app.ui.readbook.ReadBookActivity

object PendingIntentHelp {

    fun readBookActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReadBookActivity::class.java)
        intent.action = "readBookActivity"
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun aloudServicePendingIntent(context: Context, actionStr: String): PendingIntent {
        val intent = Intent(context, TTSReadAloudService::class.java)
        intent.action = actionStr
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


}