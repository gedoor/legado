package io.legado.app.ui.read

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book

class ReadViewModel(application: Application) : BaseViewModel(application) {

    val bookData = MutableLiveData<Book>()

    fun initData(intent: Intent) {
        val bookUrl = intent.getStringExtra("bookUrl")
        if (!bookUrl.isNullOrEmpty()) {

        }
    }

}