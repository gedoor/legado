package io.legado.app.ui.book.changesource

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.printOnDebug
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import splitties.init.appCtx

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.min

@Suppress("MemberVisibilityCanBePrivate")
class ChangeChapterSourceViewModel(application: Application) : BaseViewModel(application) {
    private val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    private val searchGroup get() = appCtx.getPrefString("searchGroup") ?: ""
    val searchStateData = MutableLiveData<Boolean>()
    var name: String = ""
    var author: String = ""
    var chapterIndex: Int = 0
    var chapterTitle: String = ""
    private var tasks = CompositeCoroutine()
    private var screenKey: String = ""
    private var bookSourceList = arrayListOf<BookSource>()
    private val searchBooks = Collections.synchronizedList(arrayListOf<SearchBook>())
    private val tocMap = ConcurrentHashMap<String, List<BookChapter>>()
    private var searchCallback: SourceCallback? = null
    val searchDataFlow = callbackFlow {

        searchCallback = object : SourceCallback {

            override fun searchSuccess(searchBook: SearchBook) {
                appDb.searchBookDao.insert(searchBook)
                when {
                    screenKey.isEmpty() -> searchBooks.add(searchBook)
                    searchBook.name.contains(screenKey) -> searchBooks.add(searchBook)
                    else -> return
                }
                trySend(searchBooks)
            }

            override fun upAdapter() {
                trySend(searchBooks)
            }

        }

        getDbSearchBooks().let {
            searchBooks.clear()
            searchBooks.addAll(it)
            trySend(searchBooks)
        }

        if (searchBooks.size <= 1) {
            startSearch()
        }

        awaitClose {
            searchCallback = null
        }
    }.map {
        searchBooks.sortedBy { it.originOrder }
    }.flowOn(IO)

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
            bundle.getString("chapterTitle")?.let {
                chapterTitle = it
            }
            chapterIndex = bundle.getInt("chapterIndex")
        }
    }

    private fun initSearchPool() {
        searchPool = Executors
            .newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
        searchIndex = -1
    }

    fun refresh() {
        getDbSearchBooks().let {
            searchBooks.clear()
            searchBooks.addAll(it)
            searchCallback?.upAdapter()
        }
    }

    fun startSearch() {
        execute {
            stopSearch()
            appDb.searchBookDao.clear(name, author)
            searchBooks.clear()
            bookSourceList.clear()
            if (searchGroup.isBlank()) {
                bookSourceList.addAll(appDb.bookSourceDao.allEnabled)
            } else {
                val sources = appDb.bookSourceDao.getEnabledByGroup(searchGroup)
                if (sources.isEmpty()) {
                    bookSourceList.addAll(appDb.bookSourceDao.allEnabled)
                } else {
                    bookSourceList.addAll(sources)
                }
            }
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
        }
        val source = bookSourceList[searchIndex]
        val task = WebBook
            .searchBook(viewModelScope, source, name, context = searchPool!!)
            .timeout(60000L)
            .onSuccess(IO) {
                it.forEach { searchBook ->
                    if (searchBook.name == name) {
                        if ((AppConfig.changeSourceCheckAuthor && searchBook.author.contains(author))
                            || !AppConfig.changeSourceCheckAuthor
                        ) {
                            if (searchBook.latestChapterTitle.isNullOrEmpty()) {
                                if (AppConfig.changeSourceLoadInfo || AppConfig.changeSourceLoadToc) {
                                    loadBookInfo(source, searchBook.toBook())
                                } else {
                                    searchCallback?.searchSuccess(searchBook)
                                }
                            } else {
                                searchCallback?.searchSuccess(searchBook)
                            }
                        }
                    }
                }
            }
            .onFinally(searchPool) {
                synchronized(this) {
                    if (searchIndex < bookSourceList.lastIndex) {
                        search()
                    } else {
                        searchIndex++
                    }
                    if (searchIndex >= bookSourceList.lastIndex + bookSourceList.size
                        || searchIndex >= bookSourceList.lastIndex + threadCount
                    ) {
                        searchStateData.postValue(false)
                        tasks.clear()
                    }
                }

            }
        tasks.add(task)

    }

    private fun loadBookInfo(source: BookSource, book: Book) {
        WebBook.getBookInfo(viewModelScope, source, book, context = searchPool!!)
            .onSuccess(IO) {
                if (context.getPrefBoolean(PreferKey.changeSourceLoadToc)) {
                    loadBookToc(source, book)
                } else {
                    //从详情页里获取最新章节
                    book.latestChapterTitle = it.latestChapterTitle
                    val searchBook = book.toSearchBook()
                    searchCallback?.searchSuccess(searchBook)
                }
            }.onError(IO) {
                it.printOnDebug()
            }
    }

    private fun loadBookToc(source: BookSource, book: Book) {
        WebBook.getChapterList(viewModelScope, source, book, context = searchPool!!)
            .onSuccess(IO) { chapters ->
                tocMap[book.bookUrl] = chapters
                book.latestChapterTitle = chapters.last().title
                val searchBook: SearchBook = book.toSearchBook()
                searchCallback?.searchSuccess(searchBook)
            }.onError(IO) {
                it.printOnDebug()
            }
    }

    private fun getDbSearchBooks(): List<SearchBook> {
        return if (screenKey.isEmpty()) {
            if (AppConfig.changeSourceCheckAuthor) {
                appDb.searchBookDao.getChangeSourceSearch(name, author, searchGroup)
            } else {
                appDb.searchBookDao.getChangeSourceSearch(name, "", searchGroup)
            }
        } else {
            if (AppConfig.changeSourceCheckAuthor) {
                appDb.searchBookDao.getChangeSourceSearch(name, author, screenKey, searchGroup)
            } else {
                appDb.searchBookDao.getChangeSourceSearch(name, "", screenKey, searchGroup)
            }
        }
    }

    /**
     * 筛选
     */
    fun screen(key: String?) {
        screenKey = key?.trim() ?: ""
        execute {
            getDbSearchBooks().let {
                searchBooks.clear()
                searchBooks.addAll(it)
                searchCallback?.upAdapter()
            }
        }
    }

    fun startOrStopSearch() {
        if (tasks.isEmpty) {
            startSearch()
        } else {
            stopSearch()
        }
    }

    fun stopSearch() {
        tasks.clear()
        searchPool?.close()
        searchStateData.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        searchPool?.close()
    }

    fun getToc(
        searchBook: SearchBook,
        success: (toc: List<BookChapter>) -> Unit,
        error: (msg: String) -> Unit
    ) {
        execute {
            return@execute tocMap[searchBook.bookUrl]
                ?: let {
                    val book = searchBook.toBook()
                    val source = appDb.bookSourceDao.getBookSource(book.origin)
                        ?: throw NoStackTraceException("书源不存在")
                    if (book.tocUrl.isEmpty()) {
                        WebBook.getBookInfoAwait(this, source, book)
                    }
                    val toc = WebBook.getChapterListAwait(this, source, book)
                    tocMap[book.bookUrl] = toc
                    toc
                }
        }.onSuccess {
            success(it)
        }.onError {
            error(it.localizedMessage ?: "获取目录出错")
        }
    }

    fun getContent(
        book: Book,
        chapter: BookChapter,
        nextChapterUrl: String?,
        success: (content: String) -> Unit,
        error: (msg: String) -> Unit
    ) {
        execute {
            val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                ?: throw NoStackTraceException("书源不存在")
            WebBook.getContentAwait(this, bookSource, book, chapter, nextChapterUrl, false)
        }.onSuccess {
            success.invoke(it)
        }.onError {
            error.invoke(it.localizedMessage ?: "获取正文出错")
        }
    }

    fun disableSource(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                source.enabled = false
                appDb.bookSourceDao.update(source)
            }
            searchBooks.remove(searchBook)
            searchCallback?.upAdapter()
        }
    }

    fun topSource(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                val minOrder = appDb.bookSourceDao.minOrder - 1
                source.customOrder = minOrder
                searchBook.originOrder = source.customOrder
                appDb.bookSourceDao.update(source)
                updateSource(searchBook)
            }
            searchCallback?.upAdapter()
        }
    }

    fun bottomSource(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                val maxOrder = appDb.bookSourceDao.maxOrder + 1
                source.customOrder = maxOrder
                searchBook.originOrder = source.customOrder
                appDb.bookSourceDao.update(source)
                updateSource(searchBook)
            }
            searchCallback?.upAdapter()
        }
    }

    fun updateSource(searchBook: SearchBook) {
        appDb.searchBookDao.update(searchBook)
    }

    fun del(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                appDb.bookSourceDao.delete(source)
                appDb.searchBookDao.delete(searchBook)
            }
        }
        searchBooks.remove(searchBook)
        searchCallback?.upAdapter()
    }

    fun firstSourceOrNull(searchBook: SearchBook): SearchBook? {
        return searchBooks.firstOrNull { it.bookUrl != searchBook.bookUrl }
    }

    interface SourceCallback {

        fun searchSuccess(searchBook: SearchBook)

        fun upAdapter()

    }

}