package io.legado.app.service.help

import android.content.Context
import io.legado.app.constant.IntentAction
import io.legado.app.service.DownloadService
import io.legado.app.utils.startService

object Download {

    fun start(context: Context, downloadId: Long, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("downloadId", downloadId)
            putExtra("fileName", fileName)
        }
    }

    fun stop(context: Context) {
        context.startService<DownloadService> {
            action = IntentAction.stop
        }
    }

}