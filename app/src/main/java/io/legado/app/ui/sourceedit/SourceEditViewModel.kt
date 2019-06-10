package io.legado.app.ui.sourceedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.data.entities.BookSource
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SourceEditViewModel(application: Application) : AndroidViewModel(application) {

    val sourceLiveData: MutableLiveData<BookSource> = MutableLiveData()

    fun setBookSource(key: String) {
        GlobalScope.launch {
            val source = App.db.bookSourceDao().findByKey(key)
            sourceLiveData.postValue(source)
        }
    }

    fun save(bookSource: BookSource) {
        val source = App.db.bookSourceDao().findByKey(bookSource.origin)
        if (source == null) {
            bookSource.customOrder = App.db.bookSourceDao().allCount()
        }
        App.db.bookSourceDao().insert(bookSource)
    }


}