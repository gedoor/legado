package io.legado.app.ui.book.changesource

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.CompositeCoroutine
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.getPrefBoolean
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.jetbrains.anko.debug
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import kotlin.math.min

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
            App.db.searchBookDao().getByNameAuthorEnable(name, author).let {
                searchBooks.addAll(it)
                if (it.size <= 1) {
                    upAdapter()
                    startSearch()
                } else {
                    upAdapter()
                }
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
            handler.postDelayed(sendRunnable, 500 - System.currentTimeMillis() + postTime)
        }
    }

    private fun searchFinish(searchBook: SearchBook) {
        App.db.searchBookDao().insert(searchBook)
        if (screenKey.isEmpty()) {
            searchBooks.add(searchBook)
        } else if (searchBook.name.contains(screenKey)) {
            searchBooks.add(searchBook)
        }
        upAdapter()
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
            val variableBook = SearchBook()
            val task = WebBook(source)
                .searchBook(name, variableBook = variableBook, scope = this, context = searchPool!!)
                .timeout(60000L)
                .onSuccess(IO) {
                    it.forEach { searchBook ->
                        if (searchBook.name == name && searchBook.author == author) {
                            if (searchBook.latestChapterTitle.isNullOrEmpty()) {
                                if (context.getPrefBoolean(PreferKey.changeSourceLoadInfo) || context.getPrefBoolean(PreferKey.changeSourceLoadToc)) {
                                    loadBookInfo(searchBook.toBook())
                                } else {
                                    searchFinish(searchBook)
                                }
                            } else {
                                searchFinish(searchBook)
                            }
                            return@forEach
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

    private fun loadBookInfo(book: Book) {
        execute {
            App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                WebBook(bookSource).getBookInfo(book, this)
                    .onSuccess {
                        if (context.getPrefBoolean(PreferKey.changeSourceLoadToc)) {
                            loadChapter(it)
                        } else {
                            //从详情页里获取最新章节
                            book.latestChapterTitle = it.latestChapterTitle
                            val searchBook = book.toSearchBook()
                            searchFinish(searchBook)
                        }
                    }.onError {
                        debug { context.getString(R.string.error_get_book_info) }
                    }
            } ?: debug { context.getString(R.string.error_no_source) }
        }
    }

    private fun loadChapter(book: Book) {
        execute {
            App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                WebBook(bookSource).getChapterList(book, this)
                    .onSuccess(IO) { chapters ->
                        if (chapters.isNotEmpty()) {
                            book.latestChapterTitle = chapters.last().title
                            val searchBook: SearchBook = book.toSearchBook()
                            searchFinish(searchBook)
                        }
                    }.onError {
                        debug { context.getString(R.string.error_get_chapter_list) }
                    }
            } ?: debug { R.string.error_no_source }
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
                val items = App.db.searchBookDao().getChangeSourceSearch(name, author, screenKey)
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
            App.db.bookSourceDao().getBookSource(searchBook.origin)?.let { source ->
                source.enabled = false
                App.db.bookSourceDao().update(source)
            }
            searchBooks.remove(searchBook)
            upAdapter()
        }
    }

}