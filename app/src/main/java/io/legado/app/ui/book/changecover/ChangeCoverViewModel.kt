package io.legado.app.ui.book.changecover

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.webBook.WebBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class ChangeCoverViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    var name: String = ""
    var author: String = ""
    private var task: Coroutine<*>? = null
    val searchStateData = MutableLiveData<Boolean>()
    val searchBooksLiveData = MutableLiveData<List<SearchBook>>()
    private val searchBooks = ArrayList<SearchBook>()

    fun initData(arguments: Bundle?) {
        arguments?.let { bundle ->
            bundle.getString("name")?.let {
                name = it
            }
            bundle.getString("author")?.let {
                author = it.replace(AppPattern.authorRegex, "")
            }
        }
    }

    fun loadDbSearchBook() {
        execute {
            App.db.searchBookDao().getEnableHasCover(name, author).let {
                searchBooks.addAll(it)
                if (it.size <= 1) {
                    searchBooksLiveData.postValue(searchBooks)
                    search()
                } else {
                    searchBooksLiveData.postValue(searchBooks)
                }
            }
        }
    }

    fun search() {
        task = execute {
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(name, scope = this@execute, context = searchPool)
                    .timeout(30000L)
                    .onSuccess(Dispatchers.IO) {
                        if (it.isNotEmpty()) {
                            val searchBook = it[0]
                            if (searchBook.name == name && searchBook.author == author
                                && !searchBook.coverUrl.isNullOrEmpty()
                            ) {
                                App.db.searchBookDao().insert(searchBook)
                                if (!searchBooks.contains(searchBook)) {
                                    searchBooks.add(searchBook)
                                    searchBooksLiveData.postValue(searchBooks)
                                }
                            }
                        }
                    }
            }
        }.onStart {
            searchStateData.postValue(true)
        }.onCancel {
            searchStateData.postValue(false)
        }

        task?.invokeOnCompletion {
            searchStateData.postValue(false)
        }
    }

    fun stopSearch() {
        if (task?.isActive == true) {
            task?.cancel()
        } else {
            search()
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchPool.close()
    }

}