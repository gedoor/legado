package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import io.legado.app.constant.IntentAction
import io.legado.app.service.CheckSourceService

object CheckSource {

    fun start(context: Context, selectedIds: ArrayList<String>) {
        Intent(context, CheckSourceService::class.java).let {
            it.action = IntentAction.start
            it.putExtra("selectIds", selectedIds)
            context.startService(it)
        }
    }

    fun stop(context: Context) {
        Intent(context, CheckSourceService::class.java).let {
            it.action = IntentAction.stop
            context.startService(it)
        }
    }
}