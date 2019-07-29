package io.legado.app.ui.sourceedit

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class SourceEditViewModel(application: Application) : BaseViewModel(application) {

    val sourceLiveData: MutableLiveData<BookSource> = MutableLiveData()

    fun setBookSource(key: String) {
        launch(IO) {
            App.db.bookSourceDao().findByKey(key)?.let {
                sourceLiveData.postValue(it)
            } ?: sourceLiveData.postValue(BookSource())
        }
    }

    fun save(bookSource: BookSource, finally: (() -> Unit)? = null) {
        launch(IO) {
            if (bookSource.customOrder == 0) {
                bookSource.customOrder = App.db.bookSourceDao().allCount()
            }
            App.db.bookSourceDao().insert(bookSource)
            finally?.let { it() }
        }
    }


}