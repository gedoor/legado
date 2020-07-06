package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import io.legado.app.constant.IntentAction
import io.legado.app.service.DownloadService

object Download {

    val logs = arrayListOf<String>()

    fun addLog(log: String?) {
        log ?: return
        synchronized(this) {
            if (logs.size > 1000) {
                logs.removeAt(0)
            }
            logs.add(log)
        }
    }

    fun start(context: Context, bookUrl: String, start: Int, end: Int) {
        Intent(context, DownloadService::class.java).let {
            it.action = IntentAction.start
            it.putExtra("bookUrl", bookUrl)
            it.putExtra("start", start)
            it.putExtra("end", end)
            context.startService(it)
        }
    }

    fun remove(context: Context, bookUrl: String) {
        Intent(context, DownloadService::class.java).let {
            it.action = IntentAction.remove
            it.putExtra("bookUrl", bookUrl)
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