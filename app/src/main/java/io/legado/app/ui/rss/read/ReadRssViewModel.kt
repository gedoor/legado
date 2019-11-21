package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Rss
import io.legado.app.model.analyzeRule.AnalyzeUrl

class ReadRssViewModel(application: Application) : BaseViewModel(application) {
    var rssSource: RssSource? = null
    val rssArticleLiveData = MutableLiveData<RssArticle>()
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()
    var star = false

    fun initData(intent: Intent) {
        execute {
            val origin = intent.getStringExtra("origin")
            val link = intent.getStringExtra("link")
            if (origin != null && link != null) {
                rssSource = App.db.rssSourceDao().getByKey(origin)
                star = App.db.rssStarDao().get(origin, link) != null
                App.db.rssArticleDao().get(origin, link)?.let { rssArticle ->
                    rssArticleLiveData.postValue(rssArticle)
                    if (!rssArticle.description.isNullOrBlank()) {
                        contentLiveData.postValue(rssArticle.description)
                    } else {
                        rssSource?.let {
                            val ruleContent = it.ruleContent
                            if (!ruleContent.isNullOrBlank()) {
                                loadContent(rssArticle, ruleContent)
                            } else {
                                loadUrl(rssArticle)
                            }
                        } ?: loadUrl(rssArticle)
                    }
                }
            }
        }
    }

    private fun loadUrl(rssArticle: RssArticle) {
        val analyzeUrl = AnalyzeUrl(
            rssArticle.link,
            baseUrl = rssArticle.origin,
            useWebView = true,
            headerMapF = rssSource?.getHeaderMap()
        )
        urlLiveData.postValue(analyzeUrl)
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        Rss.getContent(rssArticle, ruleContent, this)
            .onSuccess {
                contentLiveData.postValue(it)
            }
    }

    fun star() {
        execute {
            if (star) {

            }
        }
    }
}