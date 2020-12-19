package io.legado.app.model.rss

import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.RuleData
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

object Rss {

    fun getArticles(
        sortName: String,
        sortUrl: String,
        rssSource: RssSource,
        page: Int,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<RssResult> {
        return Coroutine.async(scope, context) {
            val ruleData = RuleData()
            val analyzeUrl = AnalyzeUrl(
                sortUrl,
                page = page,
                ruleData = ruleData,
                headerMapF = rssSource.getHeaderMap()
            )
            val body = analyzeUrl.getStrResponse(rssSource.sourceUrl).body
            RssParserByRule.parseXML(sortName, sortUrl, body, rssSource, ruleData)
        }
    }

    fun getContent(
        rssArticle: RssArticle,
        ruleContent: String,
        rssSource: RssSource?,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<String> {
        return Coroutine.async(scope, context) {
            val analyzeUrl = AnalyzeUrl(
                rssArticle.link,
                baseUrl = rssArticle.origin,
                ruleData = rssArticle,
                headerMapF = rssSource?.getHeaderMap()
            )
            val body = analyzeUrl.getStrResponse(rssArticle.origin).body
            val analyzeRule = AnalyzeRule(rssArticle)
            analyzeRule.setContent(body)
                .setBaseUrl(NetworkUtils.getAbsoluteURL(rssArticle.origin, rssArticle.link))
            analyzeRule.getString(ruleContent)
        }
    }
}