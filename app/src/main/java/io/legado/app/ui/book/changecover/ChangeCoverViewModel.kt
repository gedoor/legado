package io.legado.app.ui.book.changecover

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.config.AppConfig
import io.legado.app.model.BookCover
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.mapParallelSafe
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Collections
import java.util.concurrent.Executors
import kotlin.math.min

class ChangeCoverViewModel(application: Application) : BaseViewModel(application) {
    private val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    private var searchSuccess: ((SearchBook) -> Unit)? = null
    private var upAdapter: (() -> Unit)? = null
    private var bookSourceParts = arrayListOf<BookSourcePart>()
    private val defaultCover by lazy {
        listOf(
            SearchBook(
                originName = "默认封面",
                name = name,
                author = author,
                coverUrl = "use_default_cover"
            )
        )
    }
    private var task: Job? = null
    val searchStateData = MutableLiveData<Int>()
    var name: String = ""
    var author: String = ""
    val searchBooks: MutableList<SearchBook> = Collections.synchronizedList(arrayListOf())
    val dataFlow = callbackFlow {

        searchSuccess = { searchBook ->
            if (!searchBooks.contains(searchBook)) {
                searchBooks.add(searchBook)
                trySend(defaultCover + searchBooks.sortedBy { it.originOrder })
            }
        }

        upAdapter = {
            trySend(defaultCover + searchBooks.sortedBy { it.originOrder })
        }

        appDb.searchBookDao.getEnableHasCover(name, author).let {
            searchBooks.addAll(it)
            trySend(defaultCover + searchBooks.toList())
        }

        if (searchBooks.size <= 1) {
            startSearch()
        }

        awaitClose {
            searchBooks.clear()
            searchSuccess = null
            upAdapter = null
        }
    }.flowOn(IO)

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
        searchPool = Executors
            .newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    }

    private fun startSearch() {
        execute {
            stopSearch()
            if(searchStateData.value == 2) {
                upAdapter?.invoke()
                initSearchPool()
                search()
                return@execute
            }
            searchBooks.clear()
            upAdapter?.invoke()
            bookSourceParts.clear()
            bookSourceParts.addAll(appDb.bookSourceDao.allEnabledPart)
            initSearchPool()
            searchWithBookCover()
        }
    }

    private fun searchWithBookCover() {
        viewModelScope.launch(IO) {
            searchStateData.postValue(1)
            try {
                val tempBook = Book(name = name, author = author)
                val coverUrl = BookCover.searchCover(tempBook)
                if (!coverUrl.isNullOrEmpty()) {
                    val searchBook = SearchBook(
                        originName = "封面规则",
                        name = name,
                        author = author,
                        coverUrl = coverUrl,
                        originOrder = -1
                    )
                    searchSuccess?.invoke(searchBook)
                    searchStateData.postValue(2)
                } else {
                    search()
                }
            } catch (e: Exception) {
                AppLog.put("封面规则搜索出错\n${e.localizedMessage}", e)
                search()
            }
        }
    }

    private fun search() {
        task = viewModelScope.launch(searchPool!!) {
            flow {
                for (bs in bookSourceParts) {
                    bs.getBookSource()?.let {
                        emit(it)
                    }
                }
            }.onStart {
                searchStateData.postValue(1)
            }.mapParallelSafe(threadCount) {
                withTimeout(60000L) {
                    search(it)
                }
            }.onCompletion {
                searchStateData.postValue(0)
            }.catch {
                AppLog.put("封面换源搜索出错\n${it.localizedMessage}", it)
            }.collect()
        }
    }

    private suspend fun search(source: BookSource) {
        if (source.getSearchRule().coverUrl.isNullOrBlank()) {
            return
        }
        val searchBook = WebBook.searchBookAwait(
            source, name,
            shouldBreak = { it > 0 }).firstOrNull() ?: return
        if (searchBook.name == name && searchBook.author == author
            && !searchBook.coverUrl.isNullOrEmpty()
        ) {
            appDb.searchBookDao.insert(searchBook)
            searchSuccess?.invoke(searchBook)
        }
    }

    fun startOrStopSearch() {
        if (task == null || !task!!.isActive) {
            startSearch()
        } else {
            searchStateData.postValue(0)
            stopSearch()
        }
    }

    private fun stopSearch() {
        task?.cancel()
        searchPool?.close()
    }

    override fun onCleared() {
        super.onCleared()
        searchPool?.close()
    }

}