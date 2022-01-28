package io.legado.app.model

import android.content.Context
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.data.entities.BookSource
import io.legado.app.service.CheckSourceService
import io.legado.app.utils.startService
import io.legado.app.utils.toastOnUi
import io.legado.app.help.CacheManager
import splitties.init.appCtx

object CheckSource {
    var keyword = "我的"
    //校验设置
    var timeout = CacheManager.getLong("checkSourceTimeout") ?: 180000L
    var checkSearch = CacheManager.get("checkSearch")?.toBoolean() ?: true
    var checkDiscovery = CacheManager.get("checkDiscovery")?.toBoolean() ?: true
    var checkInfo = CacheManager.get("checkInfo")?.toBoolean() ?: true
    var checkCategory = CacheManager.get("checkCategory")?.toBoolean() ?: true
    var checkContent = CacheManager.get("checkContent")?.toBoolean() ?: true
    var summary = ""

    init {
        upSummary()
    }

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
        CacheManager.put("checkSourceTimeout", timeout)
        CacheManager.put("checkSearch", checkSearch)
        CacheManager.put("checkDiscovery", checkDiscovery)
        CacheManager.put("checkInfo", checkInfo)
        CacheManager.put("checkCategory", checkCategory)
        CacheManager.put("checkContent", checkContent)
        upSummary()
    }

    fun upSummary() {
        summary = ""
        if (checkSearch) summary = "${summary} ${appCtx.getString(R.string.search)}"
        if (checkDiscovery) summary = "${summary} ${appCtx.getString(R.string.discovery)}"
        if (checkInfo) summary = "${summary} ${appCtx.getString(R.string.source_tab_info)}"
        if (checkCategory) summary = "${summary} ${appCtx.getString(R.string.chapter_list)}"
        if (checkContent) summary = "${summary} ${appCtx.getString(R.string.main_body)}"
        summary = appCtx.getString(R.string.check_source_config_summary, (timeout / 1000).toString(), summary)
    }
}