package io.legado.app.model.rss

import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.RuleData
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Suppress("MemberVisibilityCanBePrivate")
object Rss {

    fun getArticles(
        scope: CoroutineScope,
        sortName: String,
        sortUrl: String,
        rssSource: RssSource,
        page: Int,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<Pair<MutableList<RssArticle>, String?>> {
        return Coroutine.async(scope, context) {
            getArticlesAwait(sortName, sortUrl, rssSource, page)
        }
    }

    suspend fun getArticlesAwait(
        sortName: String,
        sortUrl: String,
        rssSource: RssSource,
        page: Int,
    ): Pair<MutableList<RssArticle>, String?> {
        val ruleData = RuleData()
        val analyzeUrl = AnalyzeUrl(
            sortUrl,
            page = page,
            ruleData = ruleData,
            source = rssSource,
            headerMapF = rssSource.getHeaderMap()
        )
        val body = analyzeUrl.getStrResponse().body
        return RssParserByRule.parseXML(sortName, sortUrl, body, rssSource, ruleData)
    }

    fun getContent(
        scope: CoroutineScope,
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            getContentAwait(rssArticle, ruleContent, rssSource)
        }
    }

    suspend fun getContentAwait(
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource,
    ): String {
        val analyzeUrl = AnalyzeUrl(
            rssArticle.link,
            baseUrl = rssArticle.origin,
            ruleData = rssArticle,
            source = rssSource,
            headerMapF = rssSource.getHeaderMap()
        )
        val body = analyzeUrl.getStrResponse().body
        Debug.log(rssSource.sourceUrl, "≡获取成功:${rssSource.sourceUrl}")
        Debug.log(rssSource.sourceUrl, body, state = 20)
        val analyzeRule = AnalyzeRule(rssArticle, rssSource)
        analyzeRule.setContent(body)
            .setBaseUrl(NetworkUtils.getAbsoluteURL(rssArticle.origin, rssArticle.link))
        return analyzeRule.getString(ruleContent)
    }
}