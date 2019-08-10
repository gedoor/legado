package io.legado.app.ui.bookinfo

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book

class BookInfoViewModel(application: Application) : BaseViewModel(application) {

    val bookData = MutableLiveData<Book>()

}