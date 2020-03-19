package io.legado.app.model

import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class SearchBook(scope: CoroutineScope, callBack: CallBack) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    private var searchId = System.currentTimeMillis()
    private var searchPage = 1
    private var searchKey: String = ""
    private val searchEngines = arrayListOf<SearchEngine>()


    private data class SearchEngine(val bookSource: BookSource, var hasMore: Boolean = true)

    interface CallBack {
        fun onSearchSuccess()
        fun onSearchFinish()
    }

}