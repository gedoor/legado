package io.legado.app.ui.book.search

import android.os.Bundle
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.data.entities.SearchShow
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_book_search.*
import kotlinx.android.synthetic.main.view_search.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity

class SearchActivity : VMBaseActivity<SearchViewModel>(R.layout.activity_book_search),
    BookAdapter.CallBack,
    HistoryKeyAdapter.CallBack,
    SearchAdapter.CallBack {

    override val viewModel: SearchViewModel
        get() = getViewModel(SearchViewModel::class.java)

    private lateinit var adapter: SearchAdapter
    private lateinit var bookAdapter: BookAdapter
    private lateinit var historyKeyAdapter: HistoryKeyAdapter
    private var searchBookData: LiveData<PagedList<SearchShow>>? = null
    private var historyData: LiveData<List<SearchKeyword>>? = null
    private var bookData: LiveData<List<Book>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        initOtherView()
        initData()
        intent.getStringExtra("key")?.let {
            search_view.setQuery(it, true)
        }
    }

    private fun initSearchView() {
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.isSubmitButtonEnabled = true
        search_view.queryHint = getString(R.string.search_book_key)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                search_view.clearFocus()
                query?.let {
                    viewModel.saveSearchKey(query)
                    viewModel.search(it, {
                        refresh_progress_bar.isAutoLoading = true
                        initData()
                        fb_stop.visible()
                    }, {
                        refresh_progress_bar.isAutoLoading = false
                        fb_stop.invisible()
                    })
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) viewModel.stop()
                upHistory(newText)
                return false
            }
        })
        search_view.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                ll_history.visible()
            } else {
                ll_history.invisible()
            }
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        ATH.applyEdgeEffectColor(rv_bookshelf_search)
        ATH.applyEdgeEffectColor(rv_history_key)
        bookAdapter = BookAdapter(this, this)
        rv_bookshelf_search.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rv_bookshelf_search.adapter = bookAdapter
        historyKeyAdapter = HistoryKeyAdapter(this, this)
        rv_history_key.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        rv_history_key.adapter = historyKeyAdapter
        adapter = SearchAdapter(this)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }

    private fun initOtherView() {
        tv_clear_history.onClick { viewModel.clearHistory() }
        fb_stop.onClick { viewModel.stop() }
    }

    private fun initData() {
        searchBookData?.removeObservers(this)
        searchBookData = LivePagedListBuilder(
            App.db.searchBookDao().observeShow(
                viewModel.searchKey,
                viewModel.startTime
            ), 30
        ).build()
        searchBookData?.observe(this, Observer { adapter.submitList(it) })
        upHistory()
    }

    private fun upHistory(key: String? = null) {
        bookData?.removeObservers(this)
        if (key.isNullOrBlank()) {
            tv_book_show.gone()
            rv_bookshelf_search.gone()
        } else {
            bookData = App.db.bookDao().liveDataSearch(key)
            bookData?.observe(this, Observer {
                if (it.isEmpty()) {
                    tv_book_show.gone()
                    rv_bookshelf_search.gone()
                } else {
                    tv_book_show.visible()
                    rv_bookshelf_search.visible()
                }
                bookAdapter.setItems(it)
            })
        }
        historyData?.removeObservers(this)
        historyData =
            if (key.isNullOrBlank()) {
                App.db.searchKeywordDao().liveDataByUsage()
            } else {
                App.db.searchKeywordDao().liveDataSearch(key)
            }
        historyData?.observe(this, Observer { historyKeyAdapter.setItems(it) })
    }

    override fun showBookInfo(name: String, author: String) {
        viewModel.getSearchBook(name, author) { searchBook ->
            searchBook?.let {
                startActivity<BookInfoActivity>(Pair("searchBookUrl", it.bookUrl))
            }
        }
    }

    override fun showBookInfo(url: String) {
        startActivity<BookInfoActivity>(Pair("bookUrl", url))
    }

    override fun searchHistory(key: String) {
        search_view.setQuery(key, false)
    }
}
