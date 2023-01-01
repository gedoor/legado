package io.legado.app.model.webBook

import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.ui.book.search.SearchScope
import io.legado.app.utils.getPrefBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import splitties.init.appCtx
import java.util.concurrent.Executors
import kotlin.math.min

class SearchModel(private val scope: CoroutineScope, private val callBack: CallBack) {
    val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    private var mSearchId = 0L
    private var searchPage = 1
    private var searchKey: String = ""
    private var tasks = CompositeCoroutine()
    private var bookSourceList = arrayListOf<BookSource>()
    private var searchBooks = arrayListOf<SearchBook>()

    @Volatile
    private var searchIndex = -1

    private fun initSearchPool() {
        searchPool?.close()
        searchPool = Executors
            .newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    }

    fun search(searchId: Long, key: String) {
        callBack.onSearchStart()
        if (searchId != mSearchId) {
            if (key.isEmpty()) {
                callBack.onSearchCancel()
                return
            } else {
                this.searchKey = key
            }
            if (mSearchId != 0L) {
                close()
            }
            initSearchPool()
            mSearchId = searchId
            searchPage = 1
            bookSourceList.clear()
            searchBooks.clear()
            callBack.onSearchSuccess(searchBooks)
            bookSourceList.addAll(callBack.getSearchScope().getBookSources())
            if (bookSourceList.isEmpty()) {
                callBack.onSearchCancel(NoStackTraceException("启用书源为空"))
                return
            }
        } else {
            searchPage++
        }
        searchIndex = -1
        for (i in 0 until threadCount) {
            search(searchId)
        }
    }

    @Synchronized
    private fun search(searchId: Long) {
        if (searchIndex >= bookSourceList.lastIndex) {
            return
        }
        searchIndex++
        val source = bookSourceList[searchIndex]
        searchPool?.let { searchPool ->
            val task = WebBook.searchBook(
                scope,
                source,
                searchKey,
                searchPage,
                context = searchPool
            ).timeout(30000L)
                .onSuccess(searchPool) {
                    onSuccess(searchId, it)
                }
                .onFinally(searchPool) {
                    onFinally(searchId)
                }
            tasks.add(task)
        }
    }

    @Synchronized
    private fun onSuccess(searchId: Long, items: ArrayList<SearchBook>) {
        if (searchId == mSearchId) {
            appDb.searchBookDao.insert(*items.toTypedArray())
            val precision = appCtx.getPrefBoolean(PreferKey.precisionSearch)
            mergeItems(scope, items, precision)
            callBack.onSearchSuccess(searchBooks)
        }
    }

    @Synchronized
    private fun onFinally(searchId: Long) {
        if (searchIndex < bookSourceList.lastIndex) {
            search(searchId)
        } else {
            searchIndex++
        }
        if (searchIndex >= bookSourceList.lastIndex
            + min(bookSourceList.size, threadCount)
        ) {
            callBack.onSearchFinish(searchBooks.isEmpty())
        }
    }

    private fun mergeItems(scope: CoroutineScope, newDataS: List<SearchBook>, precision: Boolean) {
        if (newDataS.isNotEmpty()) {
            val copyData = ArrayList(searchBooks)
            val equalData = arrayListOf<SearchBook>()
            val containsData = arrayListOf<SearchBook>()
            val otherData = arrayListOf<SearchBook>()
            copyData.forEach {
                if (!scope.isActive) return
                if (it.name == searchKey || it.author == searchKey) {
                    equalData.add(it)
                } else if (it.name.contains(searchKey) || it.author.contains(searchKey)) {
                    containsData.add(it)
                } else {
                    otherData.add(it)
                }
            }
            newDataS.forEach { nBook ->
                if (!scope.isActive) return
                if (nBook.name == searchKey || nBook.author == searchKey) {
                    var hasSame = false
                    equalData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        equalData.add(nBook)
                    }
                } else if (nBook.name.contains(searchKey) || nBook.author.contains(searchKey)) {
                    var hasSame = false
                    containsData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        containsData.add(nBook)
                    }
                } else if (!precision) {
                    var hasSame = false
                    otherData.forEach { pBook ->
                        if (!scope.isActive) return
                        if (pBook.name == nBook.name && pBook.author == nBook.author) {
                            pBook.addOrigin(nBook.origin)
                            hasSame = true
                        }
                    }
                    if (!hasSame) {
                        otherData.add(nBook)
                    }
                }
            }
            if (!scope.isActive) return
            equalData.sortByDescending { it.origins.size }
            equalData.addAll(containsData.sortedByDescending { it.origins.size })
            if (!precision) {
                equalData.addAll(otherData)
            }
            if (!scope.isActive) return
            searchBooks = equalData
        }
    }

    fun cancelSearch() {
        close()
        callBack.onSearchCancel()
    }

    fun close() {
        tasks.clear()
        searchPool?.close()
        searchPool = null
        mSearchId = 0L
    }

    interface CallBack {
        fun getSearchScope(): SearchScope
        fun onSearchStart()
        fun onSearchSuccess(searchBooks: ArrayList<SearchBook>)
        fun onSearchFinish(isEmpty: Boolean)
        fun onSearchCancel(exception: Exception? = null)
    }

}