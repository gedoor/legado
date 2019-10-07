package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl

class ReadRssViewModel(application: Application) : BaseViewModel(application) {

    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<String>()

    fun initData(intent: Intent) {
        execute {
            intent.getStringExtra("guid")?.let {
                val rssArticle = App.db.rssArtivleDao().get(it)
                if (rssArticle != null) {
                    if (!rssArticle.description.isNullOrBlank()) {
                        contentLiveData.postValue(rssArticle.description)
                    } else {
                        App.db.rssSourceDao().getByKey(rssArticle.origin)?.let { source ->
                            val ruleContent = source.ruleContent
                            if (!ruleContent.isNullOrBlank()) {
                                loadContent(rssArticle, ruleContent)
                            } else {
                                urlLiveData.postValue(rssArticle.link)
                            }
                        } ?: let {
                            urlLiveData.postValue(rssArticle.link)
                        }
                    }
                }
            }
        }

    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        execute {
            rssArticle.link?.let {
                AnalyzeUrl(it).getResponseAsync().await().body()?.let { body ->
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