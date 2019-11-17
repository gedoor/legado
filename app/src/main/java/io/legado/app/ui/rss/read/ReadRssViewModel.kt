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
    var rssArticleLiveData = MutableLiveData<RssArticle>()
    val rssSourceLiveData = MutableLiveData<RssSource>()
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<String>()

    fun initData(intent: Intent) {
        execute {
            val origin = intent.getStringExtra("origin")
            val link = intent.getStringExtra("link")
            val rssSource = App.db.rssSourceDao().getByKey(origin)
            rssSource?.let {
                rssSourceLiveData.postValue(it)
            }
            if (origin != null && link != null) {
                App.db.rssArticleDao().get(origin, link)?.let { rssArticle ->
                    rssArticleLiveData.postValue(rssArticle)
                    if (!rssArticle.description.isNullOrBlank()) {
                        contentLiveData.postValue(rssArticle.description)
                    } else {
                        rssSource?.let {
                            val ruleContent = rssSource.ruleContent
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
        urlLiveData.postValue(NetworkUtils.getAbsoluteURL(rssArticle.origin, rssArticle.link))
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        execute {
            AnalyzeUrl(rssArticle.link, baseUrl = rssArticle.origin).getResponseAwait().body()
                ?.let { body ->
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
    }

    fun upRssArticle(rssArticle: RssArticle, success: () -> Unit) {
        execute {
            App.db.rssArticleDao().update(rssArticle)
        }.onSuccess {
            success()
        }
    }
}