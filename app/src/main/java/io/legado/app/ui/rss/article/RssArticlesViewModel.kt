package io.legado.app.ui.rss.article

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.model.rss.RssParser
import io.legado.app.model.rss.RssParserByRule
import java.net.URL


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {

    val titleLiveData = MutableLiveData<String>()

    fun loadContent(url: String, onFinally: () -> Unit) {
        execute {
            val rssSource = App.db.rssSourceDao().getByKey(url)
            rssSource?.let {
                titleLiveData.postValue(rssSource.sourceName)
            }
            val xml = URL(url).readText()
            if (rssSource == null || rssSource.ruleArticles.isNullOrBlank()) {
                RssParser.parseXML(xml, url).let {
                    App.db.rssArtivleDao().insert(*it.toTypedArray())
                }
            } else {
                RssParserByRule.parseXML(xml, rssSource).let {
                    App.db.rssArtivleDao().insert(*it.toTypedArray())
                }
            }
        }.onFinally {
            onFinally()
        }
    }

}