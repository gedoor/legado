package io.legado.app.ui.book.changecover

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.webBook.WebBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.math.min

class ChangeCoverViewModel(application: Application) : BaseViewModel(application) {
    private val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    var name: String = ""
    var author: String = ""
    private var tasks = CompositeCoroutine()
    private var bookSourceList = arrayListOf<BookSource>()
    val searchStateData = MutableLiveData<Boolean>()
    val searchBooksLiveData = MutableLiveData<List<SearchBook>>()
    private val searchBooks = ArrayList<SearchBook>()

    @Volatile
    private var searchIndex = -1

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

    private fun initSearchPool() {
        searchPool = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()
        searchIndex = -1
    }

    fun loadDbSearchBook() {
        execute {
            App.db.searchBookDao().getEnableHasCover(name, author).let {
                searchBooks.addAll(it)
                if (it.size <= 1) {
                    searchBooksLiveData.postValue(searchBooks)
                    startSearch()
                } else {
                    searchBooksLiveData.postValue(searchBooks)
                }
            }
        }
    }

    private fun startSearch() {
        execute {
            bookSourceList.clear()
            bookSourceList.addAll(App.db.bookSourceDao().allEnabled)
            searchStateData.postValue(true)
            initSearchPool()
            for (i in 0 until threadCount) {
                search()
            }
        }
    }

    private fun search() {
        synchronized(this) {
            if (searchIndex >= bookSourceList.lastIndex) {
                return
            }
            searchIndex++
            val source = bookSourceList[searchIndex]
            val variableBook = SearchBook(origin = source.bookSourceUrl)
            val task = WebBook(source)
                .searchBook(name, scope = this, context = searchPool!!, variableBook = variableBook)
                .timeout(60000L)
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
                .onFinally {
                    synchronized(this) {
                        if (searchIndex < bookSourceList.lastIndex) {
                            search()
                        } else {
                            searchIndex++
                        }
                        if (searchIndex >= bookSourceList.lastIndex + min(bookSourceList.size,
                                threadCount)
                        ) {
                            searchStateData.postValue(false)
                        }
                    }
                }
            tasks.add(task)
        }
    }

    fun stopSearch() {
        if (tasks.isEmpty) {
            startSearch()
        } else {
            tasks.clear()
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchPool?.close()
    }

}