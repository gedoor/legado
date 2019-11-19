package io.legado.app.ui.rss.article

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Rss
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {
    var rssSource: RssSource? = null
    val titleLiveData = MutableLiveData<String>()
    var page = 1

    fun loadContent(url: String, onFinally: () -> Unit, loadMore: (() -> Unit)? = null) {
        execute {
            rssSource = App.db.rssSourceDao().getByKey(url)
            rssSource?.let {
                titleLiveData.postValue(it.sourceName)
            } ?: let {
                rssSource = RssSource(sourceUrl = url)
            }
            rssSource?.let { rssSource ->
                Rss.getArticles(rssSource, page, this)
                    .onSuccess {
                        it?.let {
                            withContext(IO) {
                                App.db.rssArticleDao().insert(*it.toTypedArray())
                            }
                            loadMore?.invoke()
                        }
                    }.onError {
                        toast(it.localizedMessage)
                    }.onFinally {
                        onFinally()
                    }
            }
        }
    }

    fun read(rssArticle: RssArticle) {
        execute {
            rssArticle.read = true
            App.db.rssArticleDao().update(rssArticle)
        }
    }

    fun clear(url: String, onFinally: () -> Unit) {
        execute {
            App.db.rssArticleDao().delete(url)
            page = 1
            loadContent(url, onFinally)
        }
    }

    fun loadMore() {

    }
}