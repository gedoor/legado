package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.NetworkUtils

class ReadRssViewModel(application: Application) : BaseViewModel(application) {
    var rssArticle: RssArticle? = null
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<String>()

    fun initData(intent: Intent) {
        execute {
            val origin = intent.getStringExtra("origin")
            val title = intent.getStringExtra("title")
            if (origin != null && title != null) {
                rssArticle = App.db.rssArtivleDao().get(origin, title)
                rssArticle?.let { rssArticle ->
                    if (!rssArticle.description.isNullOrBlank()) {
                        contentLiveData.postValue(rssArticle.description)
                    } else {
                        App.db.rssSourceDao().getByKey(rssArticle.origin)?.let { source ->
                            val ruleContent = source.ruleContent
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
        rssArticle.link?.let {
            urlLiveData.postValue(NetworkUtils.getAbsoluteURL(rssArticle.origin, it))
        }
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        execute {
            rssArticle.link?.let {
                AnalyzeUrl(it, baseUrl = rssArticle.origin).getResponseAsync().await().body()
                    ?.let { body ->
                        AnalyzeRule().apply {
                            setContent(body)
                            getString(ruleContent)?.let { content ->
                                contentLiveData.postValue(content)
                            }
                        }
                    }
            }
        }
    }
}