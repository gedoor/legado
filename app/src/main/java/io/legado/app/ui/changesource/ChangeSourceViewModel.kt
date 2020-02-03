package io.legado.app.ui.changesource

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.anko.debug
import java.util.concurrent.Executors

class ChangeSourceViewModel(application: Application) : BaseViewModel(application) {
    private var searchPool =
        Executors.newFixedThreadPool(AppConfig.threadCount).asCoroutineDispatcher()
    var callBack: CallBack? = null
    val searchStateData = MutableLiveData<Boolean>()
    var name: String = ""
    var author: String = ""
    private var task: Coroutine<*>? = null
    private var screenKey: String = ""
    private val searchBooks = linkedSetOf<SearchBook>()

    fun initData() {
        execute {
            App.db.searchBookDao().getByNameAuthorEnable(name, author).let {
                searchBooks.addAll(it)
                upAdapter()
            }
        }
    }

    private fun upAdapter() {
        execute {
            callBack?.changeSourceAdapter?.let {
                val books = searchBooks.toList()
                val diffResult = DiffUtil.calculateDiff(DiffCallBack(it.getItems(), books))
                withContext(Main) {
                    synchronized(this) {
                        it.setItems(books, false)
                        diffResult.dispatchUpdatesTo(it)
                    }
                }
            }
        }
    }

    fun search() {
        task = execute {
            searchStateData.postValue(true)
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(name, scope = this@execute, context = searchPool)
                    .timeout(30000L)
                    .onSuccess(IO) {
                        it?.forEach { searchBook ->
                            if (searchBook.name == name && searchBook.author == author) {
                                if (searchBook.tocUrl.isEmpty()) {
                                    loadBookInfo(searchBook.toBook())
                                } else {
                                    loadChapter(searchBook.toBook())
                                }
                                return@onSuccess
                            }
                        }
                    }
            }
        }

        task?.invokeOnCompletion {
            searchStateData.postValue(false)
        }
    }

    private fun loadBookInfo(book: Book) {
        execute {
            App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
                WebBook(bookSource).getBookInfo(book, this)
                    .onSuccess {
                        it?.let { loadChapter(it) }
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
                    .onSuccess(IO) {
                        it?.let { chapters ->
                            if (chapters.isNotEmpty()) {
                                book.latestChapterTitle = chapters.last().title
                                val searchBook = book.toSearchBook()
                                App.db.searchBookDao().insert(searchBook)
                                searchBooks.add(searchBook)
                                upAdapter()
                            }
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
                initData()
            } else {
                App.db.searchBookDao()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchPool.close()
    }

    interface CallBack {
        var changeSourceAdapter: ChangeSourceAdapter
    }

}