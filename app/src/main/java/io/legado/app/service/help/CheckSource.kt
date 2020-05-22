package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.BookSource
import io.legado.app.service.CheckSourceService
import org.jetbrains.anko.toast

object CheckSource {

    fun start(context: Context, sources: List<BookSource>) {
        if (sources.isEmpty()) {
            context.toast(R.string.non_select)
            return
        }
        val selectedIds: ArrayList<String> = arrayListOf()
        sources.map {
            selectedIds.add(it.bookSourceUrl)
        }
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