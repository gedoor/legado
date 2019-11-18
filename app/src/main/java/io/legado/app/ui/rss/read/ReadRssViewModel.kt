package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils

class ReadRssViewModel(application: Application) : BaseViewModel(application) {
    var rssSource: RssSource? = null
    val rssArticleLiveData = MutableLiveData<RssArticle>()
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()

    fun initData(intent: Intent) {
        execute {
            val origin = intent.getStringExtra("origin")
            val link = intent.getStringExtra("link")
            rssSource = App.db.rssSourceDao().getByKey(origin)
            if (origin != null && link != null) {
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
        execute {
            val body = AnalyzeUrl(rssArticle.link, baseUrl = rssArticle.origin)
                .getResponseAwait()
                .body
            AnalyzeRule().apply {
                setContent(
                    body,
                    NetworkUtils.getAbsoluteURL(rssArticle.origin, rssArticle.link)
                )
                getString(ruleContent).let { content ->
                    contentLiveData.postValue(content)
                }
            }
        }
    }

    fun upRssArticle(rssArticle: RssArticle, success: () -> Unit) {
        execute {
            App.db.rssArticleDao().update(rssArticle)
        }.onSuccess {
            success()
        }
    }
}