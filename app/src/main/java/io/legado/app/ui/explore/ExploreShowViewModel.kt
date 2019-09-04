package io.legado.app.ui.explore

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.WebBook

class ExploreShowViewModel(application: Application) : BaseViewModel(application) {

    val booksData = MutableLiveData<List<SearchBook>>()
    var bookSource: BookSource? = null
    var page = 1

    fun initData(intent: Intent) {
        execute {
            val sourceUrl = intent.getStringExtra("sourceUrl")
            val exploreUrl = intent.getStringExtra("exploreUrl")
            if (bookSource == null) {
                bookSource = App.db.bookSourceDao().getBookSource(sourceUrl)
            }
            bookSource?.let {
                WebBook(it).exploreBook(exploreUrl, page, this)
                    .onSuccess { searchBooks ->
                        searchBooks?.let {
                            booksData.value = searchBooks
                        }
                    }
            }
        }
    }

}