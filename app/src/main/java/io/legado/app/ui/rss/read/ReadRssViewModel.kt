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
    var callBack: CallBack? = null
    var rssSource: RssSource? = null
    var rssArticle: RssArticle? = null
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
                rssArticle = App.db.rssArticleDao().get(origin, link)
                rssArticle?.let { rssArticle ->
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
        }.onFinally {
            callBack?.upStarMenu()
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
            rssArticle?.let {
                if (star) {
                    App.db.rssStarDao().delete(it.origin, it.link)
                } else {
                    App.db.rssStarDao().insert(it.toStar())
                }
            }
        }.onSuccess {
            callBack?.upStarMenu()
        }
    }

    fun clHtml(content: String): String {
        return """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style>img{max-width:100% !important; width:auto; height:auto;}</style>
            </head>
            <body style:"height:auto;max-width: 100%; width:auto;">
                $content
            </body></html>
        """
    }

    interface CallBack {
        fun upStarMenu()
    }
}