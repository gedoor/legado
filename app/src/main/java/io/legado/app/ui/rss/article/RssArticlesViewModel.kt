package io.legado.app.ui.rss.article

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Rss


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {

    val titleLiveData = MutableLiveData<String>()

    fun loadContent(url: String, onFinally: () -> Unit) {
        execute {
            var rssSource = App.db.rssSourceDao().getByKey(url)
            if (rssSource == null) {
                rssSource = RssSource(sourceUrl = url)
            } else {
                titleLiveData.postValue(rssSource.sourceName)
            }
            Rss.getArticles(rssSource, this).onSuccess {
                it?.let {
                    App.db.rssArtivleDao().insert(*it.toTypedArray())
                }
            }.onError {
                toast(it.localizedMessage)
            }.onFinally {
                onFinally()
            }
        }
    }

}