package io.legado.app.model

import io.legado.app.constant.AppConst
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RuleSub
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.http.decompressed
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.source.SourceHelp
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import java.util.concurrent.ConcurrentHashMap

object RuleUpdate {
    val cacheBookSourceMap = ConcurrentHashMap<String, List<BookSource>>()
    val cacheRssSourceMap = ConcurrentHashMap<String, List<RssSource>>()
    val cacheReplaceRuleMap = ConcurrentHashMap<String, List<ReplaceRule>>()

    suspend fun cacheSource(ruleSub: RuleSub): Boolean {
        val url = ruleSub.url
        val type = ruleSub.type
        val silentUpdate = ruleSub.silentUpdate
        val update = ruleSub.update
        val updateInterval = ruleSub.updateInterval
        if (update + updateInterval * 3600 * 1000L > System.currentTimeMillis()) {
            return false
        } else {
            ruleSub.update = System.currentTimeMillis()
            appDb.ruleSubDao.update(ruleSub)
        }
        var upRules = false
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.decompressed().byteStream().use {
            when (type) {
                0 -> GSON.fromJsonArray<BookSource>(it).getOrThrow().let { lists ->
                    val source = lists.firstOrNull() ?: return@let
                    if (source.bookSourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是书源")
                    }
                    lists.forEach { list ->
                        val localSource = appDb.bookSourceDao.getBookSourcePart(list.bookSourceUrl)
                        if (localSource == null || localSource.lastUpdateTime < list.lastUpdateTime) {
                            if (silentUpdate) {
                                if (localSource != null) {
                                    list.bookSourceGroup = localSource.bookSourceGroup
                                }
                                SourceHelp.insertBookSource(list)
                                upRules = true
                            }
                            else {
                                cacheBookSourceMap.put(url, lists)
                                return true
                            }
                        }
                    }
                }
                1 -> GSON.fromJsonArray<RssSource>(it).getOrThrow().let { lists ->
                    val source = lists.firstOrNull() ?: return@let
                    if (source.sourceUrl.isEmpty()) {
                        throw NoStackTraceException("不是订阅源")
                    }
                    lists.forEach { list ->
                        val localSource = appDb.rssSourceDao.getByKey(list.sourceUrl)
                        if (localSource == null || localSource.lastUpdateTime < list.lastUpdateTime) {
                            if (silentUpdate) {
                                if (localSource != null) {
                                    list.sourceGroup = localSource.sourceGroup
                                }
                                SourceHelp.insertRssSource(list)
                            }
                            else {
                                cacheRssSourceMap.put(url, lists)
                                return true
                            }
                        }
                    }
                }
                2 -> GSON.fromJsonArray<ReplaceRule>(it).getOrThrow().let { lists ->
                    lists.forEach { list ->
                        val oldRule = appDb.replaceRuleDao.findById(list.id)
                        if (oldRule == null || list.pattern != oldRule.pattern || list.replacement != oldRule.replacement) {
                            if (silentUpdate) {
                                appDb.replaceRuleDao.insert(list)
                                upRules = true
                            }
                            else {
                                cacheReplaceRuleMap.put(url, lists)
                                return true
                            }
                        }
                    }
                }
            }
            if (upRules) {
                ContentProcessor.upReplaceRules()
            }
        }
        return false
    }
}