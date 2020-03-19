package io.legado.app.ui.rss.article

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Rss
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {
    var callBack: CallBack? = null
    var url: String? = null
    var rssSource: RssSource? = null
    val titleLiveData = MutableLiveData<String>()
    var isLoading = true
    var order = System.currentTimeMillis()
    private var nextPageUrl: String? = null

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
            Rss.getArticles(rssSource, null)
                .onSuccess(IO) {
                    nextPageUrl = it.nextPageUrl
                    it.articles.let { list ->
                        list.forEach { rssArticle ->
                            rssArticle.order = order--
                        }
                        App.db.rssArticleDao().insert(*list.toTypedArray())
                        if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                            App.db.rssArticleDao().clearOld(url!!, order)
                            withContext(Main) {
                                callBack?.loadFinally(true)
                            }
                        } else {
                            withContext(Main) {
                                callBack?.loadFinally(false)
                            }
                        }
                        isLoading = false

                    }
                }.onError {
                    toast(it.localizedMessage)
                }
        }
    }

    fun loadMore() {
        isLoading = true
        val source = rssSource
        val pageUrl = nextPageUrl
        if (source != null && !pageUrl.isNullOrEmpty()) {
            Rss.getArticles(source, pageUrl)
                .onSuccess(IO) {
                    nextPageUrl = it.nextPageUrl
                    it.articles.let { list ->
                        if (list.isEmpty()) {
                            callBack?.loadFinally(false)
                            return@let
                        }
                        callBack?.adapter?.getItems()?.let { adapterItems ->
                            if (adapterItems.contains(list.first())) {
                                callBack?.loadFinally(false)
                            } else {
                                list.forEach { rssArticle ->
                                    rssArticle.order = order--
                                }
                                App.db.rssArticleDao().insert(*list.toTypedArray())
                            }
                        }
                    }
                    isLoading = false
                }
        } else {
            callBack?.loadFinally(false)
        }
    }

    fun read(rssArticle: RssArticle) {
        execute {
            App.db.rssArticleDao().insertRecord(RssReadRecord(rssArticle.link))
        }
    }

    fun clearArticles() {
        execute {
            url?.let {
                App.db.rssArticleDao().delete(it)
            }
            order = System.currentTimeMillis()
        }.onSuccess {
            loadContent()
        }
    }

    interface CallBack {
        var adapter: RssArticlesAdapter
        fun loadFinally(hasMore: Boolean)
    }
}