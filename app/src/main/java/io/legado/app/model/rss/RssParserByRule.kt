package io.legado.app.model.rss

import androidx.annotation.Keep
import io.legado.app.R
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.RuleDataInterface
import io.legado.app.utils.GSON
import io.legado.app.utils.NetworkUtils
import splitties.init.appCtx
import java.util.*

@Keep
object RssParserByRule {

    @Throws(Exception::class)
    fun parseXML(
        sortName: String,
        sortUrl: String,
        body: String?,
        rssSource: RssSource,
        ruleData: RuleDataInterface
    ): RssResult {
        val sourceUrl = rssSource.sourceUrl
        var nextUrl: String? = null
        if (body.isNullOrBlank()) {
            throw Exception(
                appCtx.getString(R.string.error_get_web_content, rssSource.sourceUrl)
            )
        }
        Debug.log(sourceUrl, "≡获取成功:$sourceUrl")
        Debug.log(sourceUrl, body, state = 10)
        var ruleArticles = rssSource.ruleArticles
        if (ruleArticles.isNullOrBlank()) {
            Debug.log(sourceUrl, "⇒列表规则为空, 使用默认规则解析")
            return RssParserDefault.parseXML(sortName, body, sourceUrl)
        } else {
            val articleList = mutableListOf<RssArticle>()
            val analyzeRule = AnalyzeRule(ruleData)
            analyzeRule.setContent(body).setBaseUrl(sortUrl)
            analyzeRule.setRedirectUrl(sortUrl)
            var reverse = false
            if (ruleArticles.startsWith("-")) {
                reverse = true
                ruleArticles = ruleArticles.substring(1)
            }
            Debug.log(sourceUrl, "┌获取列表")
            val collections = analyzeRule.getElements(ruleArticles)
            Debug.log(sourceUrl, "└列表大小:${collections.size}")
            if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                Debug.log(sourceUrl, "┌获取下一页链接")
                if (rssSource.ruleNextPage!!.uppercase(Locale.getDefault()) == "PAGE") {
                    nextUrl = sortUrl
                } else {
                    nextUrl = analyzeRule.getString(rssSource.ruleNextPage)
                    if (nextUrl.isNotEmpty()) {
                        nextUrl = NetworkUtils.getAbsoluteURL(sortUrl, nextUrl)
                    }
                }
                Debug.log(sourceUrl, "└$nextUrl")
            }
            val ruleTitle = analyzeRule.splitSourceRule(rssSource.ruleTitle)
            val rulePubDate = analyzeRule.splitSourceRule(rssSource.rulePubDate)
            val ruleDescription = analyzeRule.splitSourceRule(rssSource.ruleDescription)
            val ruleImage = analyzeRule.splitSourceRule(rssSource.ruleImage)
            val ruleLink = analyzeRule.splitSourceRule(rssSource.ruleLink)
            for ((index, item) in collections.withIndex()) {
                getItem(
                    sourceUrl, item, analyzeRule, index == 0,
                    ruleTitle, rulePubDate, ruleDescription, ruleImage, ruleLink
                )?.let {
                    it.sort = sortName
                    it.origin = sourceUrl
                    articleList.add(it)
                }
            }
            if (reverse) {
                articleList.reverse()
            }
            return RssResult(articleList, nextUrl)
        }
    }

    private fun getItem(
        sourceUrl: String,
        item: Any,
        analyzeRule: AnalyzeRule,
        log: Boolean,
        ruleTitle: List<AnalyzeRule.SourceRule>,
        rulePubDate: List<AnalyzeRule.SourceRule>,
        ruleDescription: List<AnalyzeRule.SourceRule>,
        ruleImage: List<AnalyzeRule.SourceRule>,
        ruleLink: List<AnalyzeRule.SourceRule>
    ): RssArticle? {
        val rssArticle = RssArticle()
        analyzeRule.setContent(item)
        Debug.log(sourceUrl, "┌获取标题", log)
        rssArticle.title = analyzeRule.getString(ruleTitle)
        Debug.log(sourceUrl, "└${rssArticle.title}", log)
        Debug.log(sourceUrl, "┌获取时间", log)
        rssArticle.pubDate = analyzeRule.getString(rulePubDate)
        Debug.log(sourceUrl, "└${rssArticle.pubDate}", log)
        Debug.log(sourceUrl, "┌获取描述", log)
        if (ruleDescription.isNullOrEmpty()) {
            rssArticle.description = null
            Debug.log(sourceUrl, "└描述规则为空，将会解析内容页", log)
        } else {
            rssArticle.description = analyzeRule.getString(ruleDescription)
            Debug.log(sourceUrl, "└${rssArticle.description}", log)
        }
        Debug.log(sourceUrl, "┌获取图片url", log)
        rssArticle.image = analyzeRule.getString(ruleImage, true)
        Debug.log(sourceUrl, "└${rssArticle.image}", log)
        Debug.log(sourceUrl, "┌获取文章链接", log)
        rssArticle.link = NetworkUtils.getAbsoluteURL(sourceUrl, analyzeRule.getString(ruleLink))
        Debug.log(sourceUrl, "└${rssArticle.link}", log)
        rssArticle.variable = GSON.toJson(analyzeRule.ruleData.variableMap)
        if (rssArticle.title.isBlank()) {
            return null
        }
        return rssArticle
    }
}