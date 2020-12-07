package io.legado.app.ui.rss.article

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.rss.Rss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RssArticlesViewModel(application: Application) : BaseViewModel(application) {
    val loadFinally = MutableLiveData<Boolean>()
    var isLoading = true
    var order = System.currentTimeMillis()
    private var nextPageUrl: String? = null
    var sortName: String = ""
    var sortUrl: String = ""
    var page = 1

    fun init(bundle: Bundle?) {
        bundle?.let {
            sortName = it.getString("sortName") ?: ""
            sortUrl = it.getString("sortUrl") ?: ""
        }
    }

    fun loadContent(rssSource: RssSource) {
        isLoading = true
        page = 1
        Rss.getArticles(sortName, sortUrl, rssSource, page)
            .onSuccess(Dispatchers.IO) {
                nextPageUrl = it.nextPageUrl
                it.articles.let { list ->
                    list.forEach { rssArticle ->
                        rssArticle.order = order--
                    }
                    App.db.rssArticleDao.insert(*list.toTypedArray())
                    if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                        App.db.rssArticleDao.clearOld(rssSource.sourceUrl, sortName, order)
                        loadFinally.postValue(true)
                    } else {
                        withContext(Dispatchers.Main) {
                            loadFinally.postValue(false)
                        }
                    }
                    isLoading = false
                }
            }.onError {
                loadFinally.postValue(false)
                it.printStackTrace()
                toast(it.localizedMessage)
            }
    }

    fun loadMore(rssSource: RssSource) {
        isLoading = true
        page++
        val pageUrl = nextPageUrl
        if (!pageUrl.isNullOrEmpty()) {
            Rss.getArticles(sortName, pageUrl, rssSource, page)
                .onSuccess(Dispatchers.IO) {
                    nextPageUrl = it.nextPageUrl
                    loadMoreSuccess(it.articles)
                }
                .onError {
                    it.printStackTrace()
                    loadFinally.postValue(false)
                }
        } else {
            loadFinally.postValue(false)
        }
    }

    private fun loadMoreSuccess(articles: MutableList<RssArticle>) {
        articles.let { list ->
            if (list.isEmpty()) {
                loadFinally.postValue(false)
                return@let
            }
            val firstArticle = list.first()
            val dbArticle = App.db.rssArticleDao
                .get(firstArticle.origin, firstArticle.link)
            if (dbArticle != null) {
                loadFinally.postValue(false)
            } else {
                list.forEach { rssArticle ->
                    rssArticle.order = order--
                }
                App.db.rssArticleDao.insert(*list.toTypedArray())
            }
        }
        isLoading = false
    }

}