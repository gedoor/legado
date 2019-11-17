package io.legado.app.model.rss

import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule

object RssParserByRule {

    @Throws(Exception::class)
    fun parseXML(body: String?, rssSource: RssSource): MutableList<RssArticle> {
        val sourceUrl = rssSource.sourceUrl
        if (body.isNullOrBlank()) {
            throw Exception(
                App.INSTANCE.getString(
                    R.string.error_get_web_content,
                    rssSource.sourceUrl
                )
            )
        }
        Debug.log(sourceUrl, "≡获取成功:$sourceUrl")
        var ruleArticles = rssSource.ruleArticles
        if (ruleArticles.isNullOrBlank()) {
            Debug.log(sourceUrl, "列表规则为空, 使用默认规则解析")
            return RssParser.parseXML(body, rssSource.sourceUrl)
        } else {
            val articleList = mutableListOf<RssArticle>()
            val analyzeRule = AnalyzeRule()
            analyzeRule.setContent(body, rssSource.sourceUrl)
            var reverse = true
            if (ruleArticles.startsWith("-")) {
                reverse = false
                ruleArticles = ruleArticles.substring(1)
            }
            Debug.log(sourceUrl, "┌获取列表")
            val collections = analyzeRule.getElements(ruleArticles)
            Debug.log(sourceUrl, "└列表大小:${collections.size}")
            val ruleTitle = analyzeRule.splitSourceRule(rssSource.ruleTitle ?: "")
            val rulePubDate = analyzeRule.splitSourceRule(rssSource.rulePubDate ?: "")
            val ruleCategories = analyzeRule.splitSourceRule(rssSource.ruleCategories ?: "")
            val ruleDescription = analyzeRule.splitSourceRule(rssSource.ruleDescription ?: "")
            val ruleImage = analyzeRule.splitSourceRule(rssSource.ruleImage ?: "")
            val ruleLink = analyzeRule.splitSourceRule(rssSource.ruleLink ?: "")
            for ((index, item) in collections.withIndex()) {
                getItem(
                    sourceUrl,
                    item,
                    analyzeRule,
                    index == 0,
                    ruleTitle,
                    rulePubDate,
                    ruleCategories,
                    ruleDescription,
                    ruleImage,
                    ruleLink
                )?.let {
                    it.origin = rssSource.sourceUrl
                    articleList.add(it)
                }
            }
            if (reverse) {
                articleList.reverse()
            }
            for ((index: Int, item: RssArticle) in articleList.withIndex()) {
                item.order = System.currentTimeMillis() + index
            }
            return articleList
        }
    }

    private fun getItem(
        sourceUrl: String,
        item: Any,
        analyzeRule: AnalyzeRule,
        log: Boolean,
        ruleTitle: List<AnalyzeRule.SourceRule>,
        rulePubDate: List<AnalyzeRule.SourceRule>,
        ruleCategories: List<AnalyzeRule.SourceRule>,
        ruleDescription: List<AnalyzeRule.SourceRule>,
        ruleImage: List<AnalyzeRule.SourceRule>,
        ruleLink: List<AnalyzeRule.SourceRule>
    ): RssArticle? {
        val rssArticle = RssArticle()
        analyzeRule.setContent(item)
        Debug.log(sourceUrl, "┌获取标题", log)
        rssArticle.title = analyzeRule.getString(ruleTitle)
        Debug.log(sourceUrl, "└${rssArticle.title}", log)
        rssArticle.pubDate = analyzeRule.getString(rulePubDate)
        rssArticle.categories = analyzeRule.getString(ruleCategories)
        rssArticle.description = analyzeRule.getString(ruleDescription)
        rssArticle.image = analyzeRule.getString(ruleImage, true)
        rssArticle.link = analyzeRule.getString(ruleLink)
        if (rssArticle.title.isBlank()) {
            return null
        }
        return rssArticle
    }
}