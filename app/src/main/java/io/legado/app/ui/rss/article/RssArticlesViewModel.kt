package io.legado.app.ui.rss.article

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.rss.RssParser
import io.legado.app.model.rss.RssParserByRule


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {

    val titleLiveData = MutableLiveData<String>()

    fun loadContent(url: String, onFinally: () -> Unit) {
        execute {
            val rssSource = App.db.rssSourceDao().getByKey(url)
            rssSource?.let {
                titleLiveData.postValue(rssSource.sourceName)
            }
            AnalyzeUrl(url).getResponseAsync().await().body()?.let { xml ->
                if (rssSource == null || rssSource.ruleArticles.isNullOrBlank()) {
                    RssParser.parseXML(xml, url).let {
                        App.db.rssArtivleDao().insert(*it.toTypedArray())
                    }
                } else {
                    RssParserByRule.parseXML(xml, rssSource).let {
                        App.db.rssArtivleDao().insert(*it.toTypedArray())
                    }
                }
            }
        }.onFinally {
            onFinally()
        }
    }

}