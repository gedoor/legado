package io.legado.app.ui.book.searchContent

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivitySearchContentBinding
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.widget.recycler.UpLinearLayoutManager
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.applyTint
import io.legado.app.utils.observeEvent
import io.legado.app.utils.postEvent
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SearchContentActivity :
    VMBaseActivity<ActivitySearchContentBinding, SearchContentViewModel>(),
    SearchContentAdapter.Callback {

    override val binding by viewBinding(ActivitySearchContentBinding::inflate)
    override val viewModel by viewModels<SearchContentViewModel>()
    private val adapter by lazy { SearchContentAdapter(this, this) }
    private val mLayoutManager by lazy { UpLinearLayoutManager(this) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private var durChapterIndex = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val bbg = bottomBackground
        val btc = getPrimaryTextColor(ColorUtils.isColorLight(bbg))
        binding.llSearchBaseInfo.setBackgroundColor(bbg)
        binding.tvCurrentSearchInfo.setTextColor(btc)
        binding.ivSearchContentTop.setColorFilter(btc)
        binding.ivSearchContentBottom.setColorFilter(btc)
        initSearchView()
        initRecyclerView()
        initView()
        intent.getStringExtra("bookUrl")?.let {
            viewModel.initBook(it) {
                initBook()
            }
        }
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (viewModel.lastQuery != query) {
                    startContentSearch(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
    }

    private fun initView() {
        binding.ivSearchContentTop.setOnClickListener {
            mLayoutManager.scrollToPositionWithOffset(
                0,
                0
            )
        }
        binding.ivSearchContentBottom.setOnClickListener {
            if (adapter.itemCount > 0) {
                mLayoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initBook() {
        binding.tvCurrentSearchInfo.text = this.getString(R.string.search_content_size) +": ${viewModel.searchResultCounts}"
        viewModel.book?.let {
            initCacheFileNames(it)
            durChapterIndex = it.durChapterIndex
            intent.getStringExtra("searchWord")?.let { searchWord ->
                searchView.setQuery(searchWord, true)
            }
        }
    }

    private fun initCacheFileNames(book: Book) {
        launch(Dispatchers.IO) {
            viewModel.cacheChapterNames.addAll(BookHelp.getChapterFiles(book))
            withContext(Dispatchers.Main) {
                adapter.notifyItemRangeChanged(0, adapter.itemCount, true)
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<BookChapter>(EventBus.SAVE_CONTENT) { chapter ->
            viewModel.book?.bookUrl?.let { bookUrl ->
                if (chapter.bookUrl == bookUrl) {
                    viewModel.cacheChapterNames.add(chapter.getFileName())
                    adapter.notifyItemChanged(chapter.index, true)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun startContentSearch(query: String) {
        // 按章节搜索内容
        if (query.isNotBlank()) {
            adapter.clearItems()
            viewModel.searchResultList.clear()
            viewModel.searchResultCounts = 0
            viewModel.lastQuery = query
            var searchResults = listOf<SearchResult>()
            launch(Dispatchers.Main) {
                appDb.bookChapterDao.getChapterList(viewModel.bookUrl).map { bookChapter ->
                    binding.refreshProgressBar.isAutoLoading = true
                    withContext(Dispatchers.IO) {
                        if (isLocalBook || viewModel.cacheChapterNames.contains(bookChapter.getFileName())) {
                            searchResults = viewModel.searchChapter(query, bookChapter)
                        }
                    }
                    if (searchResults.isNotEmpty()) {
                        viewModel.searchResultList.addAll(searchResults)
                        binding.refreshProgressBar.isAutoLoading = false
                        binding.tvCurrentSearchInfo.text = this@SearchContentActivity.getString(R.string.search_content_size) +": ${viewModel.searchResultCounts}"
                        adapter.addItems(searchResults)
                        searchResults = listOf()
                    }
                }
            }
        }
    }

    val isLocalBook: Boolean
        get() = viewModel.book?.isLocalBook() == true

    override fun openSearchResult(searchResult: SearchResult) {
        postEvent(EventBus.SEARCH_RESULT, viewModel.searchResultList as List<SearchResult>)
        val searchData = Intent()
        searchData.putExtra("searchResultIndex", viewModel.searchResultList.indexOf(searchResult))
        searchData.putExtra("chapterIndex", searchResult.chapterIndex)
        searchData.putExtra("contentPosition", searchResult.queryIndexInChapter)
        searchData.putExtra("query", searchResult.query)
        searchData.putExtra("resultCountWithinChapter", searchResult.resultCountWithinChapter)
        setResult(RESULT_OK, searchData)
        finish()
    }

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

}