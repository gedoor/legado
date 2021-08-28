package io.legado.app.ui.book.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.databinding.ActivityBookSearchBinding
import io.legado.app.lib.theme.*
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : VMBaseActivity<ActivityBookSearchBinding, SearchViewModel>(),
    BookAdapter.CallBack,
    HistoryKeyAdapter.CallBack,
    SearchAdapter.CallBack {

    override val binding by viewBinding(ActivityBookSearchBinding::inflate)
    override val viewModel by viewModels<SearchViewModel>()

    lateinit var adapter: SearchAdapter
    private lateinit var bookAdapter: BookAdapter
    private lateinit var historyKeyAdapter: HistoryKeyAdapter
    private lateinit var loadMoreView: LoadMoreView
    private lateinit var searchView: SearchView
    private var historyFlowJob: Job? = null
    private var booksFlowJob: Job? = null
    private var menu: Menu? = null
    private var precisionSearchMenuItem: MenuItem? = null
    private var groups = linkedSetOf<String>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.llHistory.setBackgroundColor(backgroundColor)
        searchView = binding.titleBar.findViewById(R.id.search_view)
        initRecyclerView()
        initSearchView()
        initOtherView()
        initData()
        receiptIntent(intent)
    }

    override fun onNewIntent(data: Intent?) {
        super.onNewIntent(data)
        receiptIntent(data)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_search, menu)
        precisionSearchMenuItem = menu.findItem(R.id.menu_precision_search)
        precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
        this.menu = menu
        upGroupMenu()
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_precision_search -> {
                putPrefBoolean(
                    PreferKey.precisionSearch,
                    !getPrefBoolean(PreferKey.precisionSearch)
                )
                precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
                searchView.query?.toString()?.trim()?.let {
                    searchView.setQuery(it, true)
                }
            }
            R.id.menu_source_manage -> startActivity<BookSourceActivity>()
            else -> if (item.groupId == R.id.source_group) {
                item.isChecked = true
                if (item.title.toString() == getString(R.string.all_source)) {
                    putPrefString("searchGroup", "")
                } else {
                    putPrefString("searchGroup", item.title.toString())
                }
                searchView.query?.toString()?.trim()?.let {
                    searchView.setQuery(it, true)
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initSearchView() {
        ATH.setTint(searchView, primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_key)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                query?.let {
                    viewModel.saveSearchKey(query)
                    viewModel.search(it)
                }
                openOrCloseHistory(false)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) viewModel.stop()
                upHistory(newText)
                return false
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus && searchView.query.toString().trim().isEmpty()) {
                finish()
            } else {
                openOrCloseHistory(hasFocus)
            }
        }
        openOrCloseHistory(true)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        ATH.applyEdgeEffectColor(binding.rvBookshelfSearch)
        ATH.applyEdgeEffectColor(binding.rvHistoryKey)
        bookAdapter = BookAdapter(this, this)
        binding.rvBookshelfSearch.layoutManager = FlexboxLayoutManager(this)
        binding.rvBookshelfSearch.adapter = bookAdapter
        historyKeyAdapter = HistoryKeyAdapter(this, this)
        binding.rvHistoryKey.layoutManager = FlexboxLayoutManager(this)
        binding.rvHistoryKey.adapter = historyKeyAdapter
        adapter = SearchAdapter(this, this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                if (toPosition == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        })
        loadMoreView = LoadMoreView(this)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun initOtherView() {
        binding.fbStop.backgroundTintList =
            Selector.colorBuild()
                .setDefaultColor(accentColor)
                .setPressedColor(ColorUtils.darkenColor(accentColor))
                .create()
        binding.fbStop.setOnClickListener {
            viewModel.stop()
            binding.refreshProgressBar.isAutoLoading = false
        }
        binding.tvClearHistory.setOnClickListener { viewModel.clearHistory() }
    }

    private fun initData() {
        launch {
            appDb.bookSourceDao.flowGroupEnabled().collect {
                groups.clear()
                it.map { group ->
                    groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                }
                upGroupMenu()
            }
        }
        viewModel.searchBookLiveData.observe(this, {
            upSearchItems(it)
        })
        viewModel.isSearchLiveData.observe(this, {
            if (it) {
                startSearch()
            } else {
                searchFinally()
            }
        })
    }

    private fun receiptIntent(intent: Intent? = null) {
        val key = intent?.getStringExtra("key")
        if (key.isNullOrBlank()) {
            searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
                .requestFocus()
        } else {
            searchView.setQuery(key, true)
        }
    }

    /**
     * 滚动到底部事件
     */
    private fun scrollToBottom() {
        if (!viewModel.isLoading && viewModel.searchKey.isNotEmpty() && loadMoreView.hasMore) {
            viewModel.search("")
        }
    }

    /**
     * 打开关闭历史界面
     */
    private fun openOrCloseHistory(open: Boolean) {
        if (open) {
            upHistory(searchView.query.toString())
            binding.llHistory.visibility = VISIBLE
        } else {
            binding.llHistory.visibility = GONE
        }
    }

    /**
     * 更新分组菜单
     */
    private fun upGroupMenu() = menu?.let { menu ->
        val selectedGroup = getPrefString("searchGroup")
        menu.removeGroup(R.id.source_group)
        val allItem = menu.add(R.id.source_group, Menu.NONE, Menu.NONE, R.string.all_source)
        var hasSelectedGroup = false
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.forEach { group ->
            menu.add(R.id.source_group, Menu.NONE, Menu.NONE, group)?.let {
                if (group == selectedGroup) {
                    it.isChecked = true
                    hasSelectedGroup = true
                }
            }
        }
        menu.setGroupCheckable(R.id.source_group, true, true)
        if (!hasSelectedGroup) {
            allItem.isChecked = true
        }
    }

    /**
     * 更新搜索历史
     */
    private fun upHistory(key: String? = null) {
        booksFlowJob?.cancel()
        booksFlowJob = launch {
            if (key.isNullOrBlank()) {
                binding.tvBookShow.gone()
                binding.rvBookshelfSearch.gone()
            } else {
                val bookFlow = appDb.bookDao.flowSearch(key)
                bookFlow.collect {
                    if (it.isEmpty()) {
                        binding.tvBookShow.gone()
                        binding.rvBookshelfSearch.gone()
                    } else {
                        binding.tvBookShow.visible()
                        binding.rvBookshelfSearch.visible()
                    }
                    bookAdapter.setItems(it)
                }
            }
        }
        historyFlowJob?.cancel()
        historyFlowJob = launch {
            when {
                key.isNullOrBlank() -> appDb.searchKeywordDao.flowByUsage()
                else -> appDb.searchKeywordDao.flowSearch(key)
            }.collect {
                historyKeyAdapter.setItems(it)
                if (it.isEmpty()) {
                    binding.tvClearHistory.invisible()
                } else {
                    binding.tvClearHistory.visible()
                }
            }
        }
    }

    /**
     * 更新搜索结果
     */
    private fun upSearchItems(items: List<SearchBook>) {
        adapter.setItems(items)
    }

    /**
     * 开始搜索
     */
    private fun startSearch() {
        binding.refreshProgressBar.isAutoLoading = true
        binding.fbStop.visible()
    }

    /**
     * 搜索结束
     */
    private fun searchFinally() {
        binding.refreshProgressBar.isAutoLoading = false
        loadMoreView.startLoad()
        binding.fbStop.invisible()
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(name: String, author: String) {
        startActivity<BookInfoActivity> {
            putExtra("name", name)
            putExtra("author", author)
        }
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(book: Book) {
        showBookInfo(book.name, book.author)
    }

    /**
     * 点击历史关键字
     */
    override fun searchHistory(key: String) {
        launch {
            when {
                searchView.query.toString() == key -> {
                    searchView.setQuery(key, true)
                }
                withContext(IO) { appDb.bookDao.findByName(key).isEmpty() } -> {
                    searchView.setQuery(key, true)
                }
                else -> {
                    searchView.setQuery(key, false)
                }
            }
        }
    }

    override fun deleteHistory(searchKeyword: SearchKeyword) {
        viewModel.deleteHistory(searchKeyword)
    }

    companion object {

        fun start(context: Context, key: String?) {
            context.startActivity<SearchActivity> {
                putExtra("key", key)
            }
        }

    }
}