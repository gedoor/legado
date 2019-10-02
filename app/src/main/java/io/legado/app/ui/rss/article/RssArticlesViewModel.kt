package io.legado.app.ui.rss.article

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.model.rss.RssParser
import java.net.URL


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {

    fun loadContent(url: String) {
        execute {
            val xml = URL(url).readText()
            RssParser.parseXML(xml).let {
                it.forEach { rssArticle ->
                    rssArticle.origin = url
                }
                App.db.rssArtivleDao().insert(*it.toTypedArray())
            }
        }
    }
}