package io.legado.app.ui.changesource

import android.app.Application
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.anko.debug

class ChangeSourceViewModel(application: Application) : BaseViewModel(application) {
    var callBack: CallBack? = null
    var name: String = ""
    var author: String = ""
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
            callBack?.adapter()?.let {
                val books = searchBooks.toList()
                books.sorted()
                val diffResult = DiffUtil.calculateDiff(DiffCallBack(it.getItems(), books))
                withContext(Main) {
                    synchronized(this) {
                        it.setItemsNoNotify(books)
                        diffResult.dispatchUpdatesTo(it)
                    }
                }
            }
        }
    }

    fun search() {
        execute {
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(name, scope = this@execute)
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
                delay(100)
            }
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
        screenKey = key ?: ""
        if (key.isNullOrEmpty()) {
            initData()
        } else {

        }
    }

    interface CallBack {
        fun adapter(): ChangeSourceAdapter
    }
}