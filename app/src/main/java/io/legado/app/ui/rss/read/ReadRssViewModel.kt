package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel

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
                    }
                }
            }
        }

    }

}