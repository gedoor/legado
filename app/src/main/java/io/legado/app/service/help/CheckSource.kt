package io.legado.app.service.help

import android.content.Context
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.BookSource
import io.legado.app.service.CheckSourceService
import io.legado.app.utils.startService
import io.legado.app.utils.toastOnUi

object CheckSource {
    var keyword = "我的"

    fun start(context: Context, sources: List<BookSource>) {
        if (sources.isEmpty()) {
            context.toastOnUi(R.string.non_select)
            return
        }
        val selectedIds: ArrayList<String> = arrayListOf()
        sources.map {
            selectedIds.add(it.bookSourceUrl)
        }
        context.startService<CheckSourceService> {
            action = IntentAction.start
            putExtra("selectIds", selectedIds)
        }
    }

    fun stop(context: Context) {
        context.startService<CheckSourceService> {
            action = IntentAction.stop
        }
    }
}