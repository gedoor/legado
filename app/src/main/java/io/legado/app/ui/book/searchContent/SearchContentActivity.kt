package io.legado.app.ui.book.searchContent

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivitySearchContentBinding
import io.legado.app.help.IntentData
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isLocal
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.widget.recycler.UpLinearLayoutManager
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
    private var searchJob: Job? = null

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
        val searchResultList = IntentData.get<List<SearchResult>>("searchResultList")
        val submit = searchResultList == null
        intent.getStringExtra("bookUrl")?.let { bookUrl ->
            viewModel.initBook(bookUrl) {
                searchResultList?.let {
                    viewModel.searchResultList.addAll(it)
                    viewModel.searchResultCounts = it.size
                    adapter.setItems(it)
                    val position = intent.getIntExtra("searchResultIndex", 0)
                    binding.recyclerView.scrollToPosition(position)
                }
                initBook(submit)
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
                startContentSearch(query.trim())
                searchView.clearFocus()
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
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
            mLayoutManager.scrollToPositionWithOffset(0, 0)
        }
        binding.ivSearchContentBottom.setOnClickListener {
            if (adapter.itemCount > 0) {
                mLayoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
            }
        }
        binding.fbStop.setOnClickListener {
            searchJob?.cancel()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initBook(submit: Boolean = true) {
        binding.tvCurrentSearchInfo.text =
            this.getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
        viewModel.book?.let {
            initCacheFileNames(it)
            durChapterIndex = it.durChapterIndex
            intent.getStringExtra("searchWord")?.let { searchWord ->
                searchView.setQuery(searchWord, submit)
            }
        }
    }

    private fun initCacheFileNames(book: Book) {
        launch {
            withContext(IO) {
                viewModel.cacheChapterNames.addAll(BookHelp.getChapterFiles(book))
            }
            adapter.notifyItemRangeChanged(0, adapter.itemCount, true)
        }
    }

    override fun observeLiveBus() {
        observeEvent<Pair<Book, BookChapter>>(EventBus.SAVE_CONTENT) { (book, chapter) ->
            viewModel.book?.bookUrl?.let { bookUrl ->
                if (book.bookUrl == bookUrl) {
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
            searchJob?.cancel()
            adapter.clearItems()
            viewModel.searchResultList.clear()
            viewModel.searchResultCounts = 0
            viewModel.lastQuery = query
            searchJob = launch {
                kotlin.runCatching {
                    withContext(IO) {
                        appDb.bookChapterDao.getChapterList(viewModel.bookUrl)
                    }.forEach { bookChapter ->
                        ensureActive()
                        binding.refreshProgressBar.isAutoLoading = true
                        binding.fbStop.visible()
                        val searchResults = withContext(IO) {
                            if (isLocalBook || viewModel.cacheChapterNames.contains(bookChapter.getFileName())) {
                                viewModel.searchChapter(query, bookChapter)
                            } else {
                                null
                            }
                        }
                        binding.tvCurrentSearchInfo.text =
                            this@SearchContentActivity.getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
                        ensureActive()
                        if (searchResults != null && searchResults.isNotEmpty()) {
                            viewModel.searchResultList.addAll(searchResults)
                            binding.refreshProgressBar.isAutoLoading = false
                            adapter.addItems(searchResults)
                        }
                    }
                    binding.refreshProgressBar.isAutoLoading = false
                    if (viewModel.searchResultCounts == 0) {
                        val noSearchResult =
                            SearchResult(resultText = getString(R.string.search_content_empty))
                        adapter.addItem(noSearchResult)
                    }
                }.onFailure {
                    binding.fbStop.invisible()
                    binding.refreshProgressBar.isAutoLoading = false
                    AppLog.put("全文搜索出错\n${it.localizedMessage}", it)
                }.onSuccess {
                    binding.fbStop.invisible()
                    binding.refreshProgressBar.isAutoLoading = false
                }
            }
        }
    }

    private val isLocalBook: Boolean
        get() = viewModel.book?.isLocal == true

    override fun openSearchResult(searchResult: SearchResult, index: Int) {
        postEvent(EventBus.SEARCH_RESULT, viewModel.searchResultList as List<SearchResult>)
        val searchData = Intent()
        val key = System.currentTimeMillis()
        IntentData.put("searchResult$key", searchResult)
        IntentData.put("searchResultList$key", viewModel.searchResultList)
        searchData.putExtra("key", key)
        searchData.putExtra("index", index)
        setResult(RESULT_OK, searchData)
        finish()
    }

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

}