package io.legado.app.model.webBook

import io.legado.app.App
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import kotlin.math.min

class SearchBookModel(private val scope: CoroutineScope, private val callBack: CallBack) {
    val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    private var mSearchId = 0L
    private var searchPage = 1
    private var searchKey: String = ""
    private var tasks = CompositeCoroutine()
    private var bookSourceList = arrayListOf<BookSource>()

    @Volatile
    private var searchIndex = -1

    private fun initSearchPool() {
        searchPool = Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()
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
            val searchGroup = App.INSTANCE.getPrefString("searchGroup") ?: ""
            bookSourceList.clear()
            if (searchGroup.isBlank()) {
                bookSourceList.addAll(App.db.bookSourceDao.allEnabled)
            } else {
                bookSourceList.addAll(App.db.bookSourceDao.getEnabledByGroup(searchGroup))
            }
        } else {
            searchPage++
        }
        searchIndex = -1
        for (i in 0 until threadCount) {
            search(searchId)
        }
    }

    private fun search(searchId: Long) {
        synchronized(this) {
            if (searchIndex >= bookSourceList.lastIndex) {
                return
            }
            searchIndex++
            val source = bookSourceList[searchIndex]
            val task = WebBook(source).searchBook(
                scope,
                searchKey,
                searchPage,
                context = searchPool!!
            ).timeout(30000L)
                .onSuccess(IO) {
                    if (searchId == mSearchId) {
                        callBack.onSearchSuccess(it)
                    }
                }
                .onFinally {
                    synchronized(this) {
                        if (searchIndex < bookSourceList.lastIndex) {
                            search(searchId)
                        } else {
                            searchIndex++
                        }
                        if (searchIndex >= bookSourceList.lastIndex + min(bookSourceList.size,
                                threadCount)
                        ) {
                            callBack.onSearchFinish()
                        }
                    }
                }
            tasks.add(task)
        }
    }

    fun cancelSearch() {
        close()
        callBack.onSearchCancel()
    }

    fun close() {
        tasks.clear()
        searchPool?.close()
        mSearchId = 0L
    }

    interface CallBack {
        fun onSearchStart()
        fun onSearchSuccess(searchBooks: ArrayList<SearchBook>)
        fun onSearchFinish()
        fun onSearchCancel()
    }

}