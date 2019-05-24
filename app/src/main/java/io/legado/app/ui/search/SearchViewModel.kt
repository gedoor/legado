package io.legado.app.ui.search

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.SearchBook

class SearchViewModel(application: Application) : BaseViewModel(application){

    val searchBooks: LiveData<List<SearchBook>> = MutableLiveData()

    public fun search(){


    }

}
