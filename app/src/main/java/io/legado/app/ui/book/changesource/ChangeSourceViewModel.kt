package io.legado.app.ui.book.changesource

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import splitties.init.appCtx
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

class ChangeSourceViewModel(application: Application) : BaseViewModel(application) {
    private val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    val handler = Handler(Looper.getMainLooper())
    val searchStateData = MutableLiveData<Boolean>()
    val searchBooksLiveData = MutableLiveData<List<SearchBook>>()
    var name: String = ""
    var author: String = ""
    private var tasks = CompositeCoroutine()
    private var screenKey: String = ""
    private var bookSourceList = arrayListOf<BookSource>()
    private val searchBooks = CopyOnWriteArraySet<SearchBook>()
    private var postTime = 0L
    private val sendRunnable = Runnable { upAdapter() }
    private val searchGroup get() = appCtx.getPrefString("searchGroup") ?: ""

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
            searchBooks.clear()
            upAdapter()
            val sbs = if (AppConfig.changeSourceCheckAuthor) {
                appDb.searchBookDao.getChangeSourceSearch(name, author, searchGroup)
            } else {
                appDb.searchBookDao.getChangeSourceSearch(name, "", searchGroup)
            }
            searchBooks.addAll(sbs)
            searchBooksLiveData.postValue(searchBooks.toList())
            if (sbs.size <= 1) {
                startSearch()
            }
        }
    }

    @Synchronized
    private fun upAdapter() {
        if (System.currentTimeMillis() >= postTime + 500) {
            handler.removeCallbacks(sendRunnable)
            postTime = System.currentTimeMillis()
            val books = searchBooks.toList()
            searchBooksLiveData.postValue(books.sortedBy { it.originOrder })
        } else {
            handler.removeCallbacks(sendRunnable)
            handler.postDelayed(sendRunnable, 500)
        }
    }

    private fun searchFinish(searchBook: SearchBook) {
        if (searchBooks.contains(searchBook)) return
        appDb.searchBookDao.insert(searchBook)
        if (screenKey.isEmpty()) {
            searchBooks.add(searchBook)
        } else if (searchBook.name.contains(screenKey)) {
            searchBooks.add(searchBook)
        }
        upAdapter()
    }

    private fun startSearch() {
        execute {
            appDb.searchBookDao.clear(name, author)
            searchBooks.clear()
            upAdapter()
            bookSourceList.clear()
            if (searchGroup.isBlank()) {
                bookSourceList.addAll(appDb.bookSourceDao.allEnabled)
            } else {
                bookSourceList.addAll(appDb.bookSourceDao.getEnabledByGroup(searchGroup))
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
        val webBook = WebBook(source)
        val task = webBook
            .searchBook(this, name, context = searchPool!!)
            .timeout(60000L)
            .onSuccess(IO) {
                it.forEach { searchBook ->
                    if (searchBook.name == name) {
                        if ((AppConfig.changeSourceCheckAuthor && searchBook.author.contains(author))
                            || !AppConfig.changeSourceCheckAuthor
                        ) {
                            if (searchBook.latestChapterTitle.isNullOrEmpty()) {
                                if (AppConfig.changeSourceLoadInfo || AppConfig.changeSourceLoadToc) {
                                    loadBookInfo(webBook, searchBook.toBook())
                                } else {
                                    searchFinish(searchBook)
                                }
                            } else {
                                searchFinish(searchBook)
                            }
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
                    if (searchIndex >= bookSourceList.lastIndex + bookSourceList.size
                        || searchIndex >= bookSourceList.lastIndex + threadCount
                    ) {
                        searchStateData.postValue(false)
                    }
                }
            }
        tasks.add(task)
    }

    private fun loadBookInfo(webBook: WebBook, book: Book) {
        webBook.getBookInfo(this, book)
            .onSuccess {
                if (context.getPrefBoolean(PreferKey.changeSourceLoadToc)) {
                    loadBookToc(webBook, book)
                } else {
                    //从详情页里获取最新章节
                    book.latestChapterTitle = it.latestChapterTitle
                    val searchBook = book.toSearchBook()
                    searchFinish(searchBook)
                }
            }.onError {
                it.printStackTrace()
            }
    }

    private fun loadBookToc(webBook: WebBook, book: Book) {
        webBook.getChapterList(this, book)
            .onSuccess(IO) { chapters ->
                if (chapters.isNotEmpty()) {
                    book.latestChapterTitle = chapters.last().title
                    val searchBook: SearchBook = book.toSearchBook()
                    searchFinish(searchBook)
                }
            }.onError {
                it.printStackTrace()
            }
    }

    /**
     * 筛选
     */
    fun screen(key: String?) {
        execute {
            screenKey = key ?: ""
            if (key.isNullOrEmpty()) {
                loadDbSearchBook()
            } else {
                val items =
                    appDb.searchBookDao.getChangeSourceSearch(name, author, screenKey, searchGroup)
                searchBooks.clear()
                searchBooks.addAll(items)
                upAdapter()
            }
        }
    }

    fun stopSearch() {
        if (tasks.isEmpty) {
            startSearch()
        } else {
            tasks.clear()
            searchPool?.close()
            searchStateData.postValue(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchPool?.close()
    }

    fun disableSource(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                source.enabled = false
                appDb.bookSourceDao.update(source)
            }
            searchBooks.remove(searchBook)
            upAdapter()
        }
    }

}