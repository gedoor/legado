package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import io.legado.app.constant.Action
import io.legado.app.service.DownloadService

object Download {

    fun start(context: Context, bookUrl: String, start: Int, end: Int) {
        Intent(context, DownloadService::class.java).let {
            it.action = Action.start
            it.putExtra("bookUrl", bookUrl)
            it.putExtra("start", start)
            it.putExtra("end", end)
            context.startService(it)
        }
    }

}