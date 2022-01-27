package io.legado.app.model

import android.content.Context
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.BookSource
import io.legado.app.service.CheckSourceService
import io.legado.app.utils.startService
import io.legado.app.utils.toastOnUi
import io.legado.app.help.CacheManager

object CheckSource {
    var keyword = "我的"
    //校验设置
    var timeout = CacheManager.getLong("checkSourceTimeout") ?: 180000L
    var checkSearch = CacheManager.get("checkSearch")?.toBoolean() ?: true
    var checkDiscovery = CacheManager.get("checkDiscovery")?.toBoolean() ?: true
    var checkInfo = CacheManager.get("checkInfo")?.toBoolean() ?: true
    var checkCategory = CacheManager.get("checkCategory")?.toBoolean() ?: true
    var checkContent = CacheManager.get("checkContent")?.toBoolean() ?: true

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

    fun putConfig() {
        CacheManager.put("checkSourceTimeout", timeout * 1000)
        CacheManager.put("checkSearch", checkSearch)
        CacheManager.put("checkDiscovery", checkDiscovery)
        CacheManager.put("checkInfo", checkInfo)
        CacheManager.put("checkCategory", checkCategory)
        CacheManager.put("checkContent", checkContent)
    }
}