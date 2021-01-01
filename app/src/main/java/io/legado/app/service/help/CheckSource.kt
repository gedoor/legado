package io.legado.app.service.help

import android.content.Context
import android.content.Intent
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.BookSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.CheckSourceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.anko.toast
import kotlin.coroutines.CoroutineContext

class CheckSource(val source: BookSource) {

    companion object {
        var keyword = "我的"

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

    fun check(
        scope: CoroutineScope,
        context: CoroutineContext,
        onNext: (sourceUrl: String) -> Unit
    ): Coroutine<*> {
        val webBook = WebBook(source)
        return webBook
            .searchBook(scope, keyword, context = context)
            .timeout(60000L)
            .onError(Dispatchers.IO) {
                source.addGroup("失效")
                App.db.bookSourceDao.update(source)
            }.onSuccess(Dispatchers.IO) {
                source.removeGroup("失效")
                App.db.bookSourceDao.update(source)
            }.onFinally(Dispatchers.IO) {
                onNext(source.bookSourceUrl)
            }
    }
}