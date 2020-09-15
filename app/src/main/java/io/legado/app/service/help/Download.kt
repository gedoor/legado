package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import io.legado.app.constant.IntentAction
import io.legado.app.service.DownloadService

object Download {

    fun start(context: Context, downloadId: Long, fileName: String) {
        Intent(context, DownloadService::class.java).let {
            it.action = IntentAction.start
            it.putExtra("downloadId", downloadId)
            it.putExtra("fileName", fileName)
            context.startService(it)
        }
    }

    fun stop(context: Context) {
        Intent(context, DownloadService::class.java).let {
            it.action = IntentAction.stop
            context.startService(it)
        }
    }

}