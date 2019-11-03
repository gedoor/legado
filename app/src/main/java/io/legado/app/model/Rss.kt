package io.legado.app.model

import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.rss.RssParserByRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

object Rss {

    fun getArticles(
        rssSource: RssSource,
        scope: CoroutineScope = Coroutine.DEFAULT,
        context: CoroutineContext = Dispatchers.IO
    ): Coroutine<MutableList<RssArticle>> {
        return Coroutine.async(scope, context) {
            val response = AnalyzeUrl(rssSource.sourceUrl).getResponseAwait()
            RssParserByRule.parseXML(response, rssSource)
        }
    }
}