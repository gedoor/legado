package io.legado.app.ui.changesource

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.SearchBook

class ChangeSourceViewModel(application: Application) : BaseViewModel(application) {
    var curBookUrl = ""
    var name: String = ""
    var author: String = ""
    val searchBookData = MutableLiveData<List<SearchBook>>()

    fun startSearch() {
        execute {
            App.db.searchBookDao().getByNameAuthorEnable(name, author).let {
                searchBookData.postValue(it)
            }
        }
    }
}