package io.legado.app.ui.rss.article

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Rss


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {
    var callBack: CallBack? = null
    var url: String? = null
    var rssSource: RssSource? = null
    val titleLiveData = MutableLiveData<String>()
    private val articles = linkedSetOf<RssArticle>()
    var isLoading = true
    var page = 1
    var hasMore = true

    fun initData(intent: Intent, finally: () -> Unit) {
        execute {
            url = intent.getStringExtra("url")
            url?.let { url ->
                rssSource = App.db.rssSourceDao().getByKey(url)
                rssSource?.let {
                    titleLiveData.postValue(it.sourceName)
                } ?: let {
                    rssSource = RssSource(sourceUrl = url)
                }
            }
        }.onFinally {
            finally()
        }
    }

    fun loadContent() {
        isLoading = true
        rssSource?.let { rssSource ->
            Rss.getArticles(rssSource, page, this)
                .onSuccess {
                    it?.let {
                        val oldSize = articles.size
                        articles.addAll(it)
                        if (articles.size == oldSize) {
                            hasMore = false
                        } else {
                            callBack?.adapter?.setItems(articles.toList())
                        }
                        page++
                        isLoading = false
                    }
                }.onError {
                    toast(it.localizedMessage)
                }
        }
    }

    fun read(rssArticle: RssArticle) {
        execute {
            rssArticle.read = true
            App.db.rssArticleDao().update(rssArticle)
        }
    }

    fun clear() {
        page = 1
        articles.clear()
        loadContent()
    }

    interface CallBack {
        var adapter: RssArticlesAdapter
        fun loadFinally()
    }
}