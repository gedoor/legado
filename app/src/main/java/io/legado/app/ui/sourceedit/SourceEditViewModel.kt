package io.legado.app.ui.sourceedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.data.dao.BookSourceDao
import io.legado.app.data.entities.BookSource

class SourceEditViewModel(application: Application) : AndroidViewModel(application) {

    val sourceLiveData:MutableLiveData<BookSource> = MutableLiveData()

    fun setBookSource(key: String) {
        sourceLiveData.value = App.db.bookSourceDao().findByKey(key)
    }



}