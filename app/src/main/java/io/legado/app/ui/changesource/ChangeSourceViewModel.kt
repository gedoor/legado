package io.legado.app.ui.changesource

import android.app.Application
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.debug

class ChangeSourceViewModel(application: Application) : BaseViewModel(application) {
    var callBack: CallBack? = null
    var curBookUrl = ""
    var name: String = ""
    var author: String = ""
    private val searchBooks = linkedSetOf<SearchBook>()

    fun initData() {
        execute {
            App.db.searchBookDao().getByNameAuthorEnable(name, author).let {
                searchBooks.addAll(it)
                upAdapter()
                search()
            }
        }
    }

    private fun upAdapter() {
        callBack?.adapter()?.let {
            val books = searchBooks.toList()
            val diffResult = DiffUtil.calculateDiff(DiffCallBack(it.getItems(), searchBooks.toList()))
            launch {
                it.setItemsNoNotify(books)
                diffResult.dispatchUpdatesTo(it)
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
                    .onSuccess(Dispatchers.IO) {
                        it?.let { list ->
                            for (searchBook in list) {
                                if (searchBook.name == name && searchBook.author == author) {
                                    if (searchBook.tocUrl.isEmpty()) {
                                        loadBookInfo(searchBook.toBook())
                                    } else {
                                        loadChapter(searchBook.toBook())
                                    }
                                    break
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun loadBookInfo(book: Book) {
        App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
            WebBook(bookSource).getBookInfo(book, this)
                .onSuccess {
                    it?.let { loadChapter(it) }
                }.onError {
                    debug { context.getString(R.string.error_get_book_info) }
                }
        } ?: debug { context.getString(R.string.error_no_source) }
    }

    private fun loadChapter(book: Book) {
        App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
            WebBook(bookSource).getChapterList(book, this)
                .onSuccess(Dispatchers.IO) {
                    it?.map { chapter ->
                        book.latestChapterTitle = chapter.title
                        val searchBook = book.toSearchBook()
                        searchBooks.add(searchBook)
                        upAdapter()
                        App.db.searchBookDao().insert(searchBook)
                    }
                }.onError {
                    debug { context.getString(R.string.error_get_chapter_list) }
                }
        } ?: debug { R.string.error_no_source }
    }

    fun screen(key: String?) {
        if (key.isNullOrEmpty()) {

        } else {

        }
    }

    interface CallBack {
        fun adapter(): ChangeSourceAdapter
    }
}