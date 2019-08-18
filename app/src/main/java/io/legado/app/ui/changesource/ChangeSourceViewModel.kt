package io.legado.app.ui.changesource

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers

class ChangeSourceViewModel(application: Application) : BaseViewModel(application) {
    var curBookUrl = ""
    var name: String = ""
    var author: String = ""
    val searchBookData = MutableLiveData<List<SearchBook>>()
    private val searchBooks = arrayListOf<SearchBook>()

    fun initData() {
        execute {
            App.db.searchBookDao().getByNameAuthorEnable(name, author).let {
                searchBooks.addAll(it)
                searchBookData.postValue(searchBooks)
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
                            list.map { searchBook ->
                                if (searchBook.name == name && searchBook.author == author) {
                                    if (searchBook.tocUrl.isEmpty()) {
                                        loadBookInfo(searchBook.toBook())
                                    } else {
                                        loadChapter(searchBook.toBook())
                                    }
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
                    toast(R.string.error_get_book_info)
                }
        } ?: toast(R.string.error_no_source)
    }

    private fun loadChapter(book: Book) {
        App.db.bookSourceDao().getBookSource(book.origin)?.let { bookSource ->
            WebBook(bookSource).getChapterList(book, this)
                .onSuccess(Dispatchers.IO) {
                    it?.let {
                        if (it.isNotEmpty()) {
                            book.latestChapterTitle = it.last().title
                            searchBooks.add(book.toSearchBook())
                        }
                    }
                }.onError {
                    toast(R.string.error_get_chapter_list)
                }
        } ?: toast(R.string.error_no_source)
    }

    fun screen(key: String?) {
        if (key.isNullOrEmpty()) {
            searchBookData.postValue(searchBooks)
        } else {

        }
    }
}