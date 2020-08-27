package io.legado.app.model.webBook

import io.legado.app.App
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class SearchBookModel(private val scope: CoroutineScope, private val callBack: CallBack) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    private var mSearchId = System.currentTimeMillis()
    private var searchPage = 1
    private var searchKey: String = ""
    private var task: Coroutine<*>? = null

    private fun initSearchPool() {
        searchPool =
            Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    }

    fun search(searchId: Long, key: String) {
        if (searchId != mSearchId) {
            task?.cancel()
            searchPool.close()
            initSearchPool()
            mSearchId = searchId
            searchPage = 1
            if (key.isEmpty()) {
                return
            } else {
                this.searchKey = key
            }
        } else {
            searchPage++
        }
        task = Coroutine.async(scope, searchPool) {
            val searchGroup = App.INSTANCE.getPrefString("searchGroup") ?: ""
            val bookSourceList = if (searchGroup.isBlank()) {
                App.db.bookSourceDao().allEnabled
            } else {
                App.db.bookSourceDao().getEnabledByGroup(searchGroup)
            }
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(
                        searchKey,
                        searchPage,
                        scope = this,
                        context = searchPool
                    )
                    .timeout(30000L)
                    .onSuccess(IO) {
                        if (searchId == mSearchId) {
                            callBack.onSearchSuccess(it)
                        }
                    }
            }
        }.onStart {
            callBack.onSearchStart()
        }

        task?.invokeOnCompletion {
            if (searchId == mSearchId) {
                callBack.onSearchFinish()
            }
        }
    }

    fun cancelSearch() {
        task?.cancel()
        mSearchId = 0
        callBack.onSearchCancel()
    }

    fun close() {
        task?.cancel()
        mSearchId = 0
        searchPool.close()
    }

    interface CallBack {
        fun onSearchStart()
        fun onSearchSuccess(searchBooks: ArrayList<SearchBook>)
        fun onSearchFinish()
        fun onSearchCancel()
    }

}