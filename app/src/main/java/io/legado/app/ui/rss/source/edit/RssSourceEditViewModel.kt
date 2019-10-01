package io.legado.app.ui.rss.source.edit

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource

class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {

    val sourceLiveData: MutableLiveData<RssSource> = MutableLiveData()

    fun setSource(key: String) {
        execute {
            App.db.rssSourceDao().getByKey(key)?.let {
                sourceLiveData.postValue(it)
            } ?: sourceLiveData.postValue(RssSource())
        }
    }


}