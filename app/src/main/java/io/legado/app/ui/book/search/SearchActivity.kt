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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.databinding.ActivityBookSearchBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.*
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx

class SearchActivity : VMBaseActivity<ActivityBookSearchBinding, SearchViewModel>(),
    BookAdapter.CallBack,
    HistoryKeyAdapter.CallBack,
    SearchAdapter.CallBack {

    override val binding by viewBinding(ActivityBookSearchBinding::inflate)
    override val viewModel by viewModels<SearchViewModel>()

    private val adapter by lazy { SearchAdapter(this, this) }
    private val bookAdapter by lazy {
        BookAdapter(this, this).apply {
            setHasStableIds(true)
        }
    }
    private val historyKeyAdapter by lazy {
        HistoryKeyAdapter(this, this).apply {
            setHasStableIds(true)
        }
    }
    private val loadMoreView by lazy { LoadMoreView(this) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private var historyFlowJob: Job? = null
    private var booksFlowJob: Job? = null
    private var menu: Menu? = null
    private var precisionSearchMenuItem: MenuItem? = null
    private var groups = linkedSetOf<String>()
    private var isManualStopSearch = false
    private val searchFinishCallback: (isEmpty: Boolean) -> Unit = searchFinish@{ isEmpty ->
        val searchGroup = AppConfig.searchGroup
        if (!isEmpty || searchGroup.isEmpty()) return@searchFinish
        launch {
            alert("搜索结果为空") {
                val precisionSearch = appCtx.getPrefBoolean(PreferKey.precisionSearch)
                if (precisionSearch) {
                    setMessage("${searchGroup}分组搜索结果为空，是否关闭精准搜索？")
                    yesButton {
                        appCtx.putPrefBoolean(PreferKey.precisionSearch, false)
                        precisionSearchMenuItem?.isChecked = false
                        viewModel.searchKey = ""
                        viewModel.search(searchView.query.toString())
                    }
                } else {
                    setMessage("${searchGroup}分组搜索结果为空，是否切换到全部分组？")
                    yesButton {
                        AppConfig.searchGroup = ""
                        viewModel.searchKey = ""
                        viewModel.search(searchView.query.toString())
                    }
                }
                noButton()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.llHistory.setBackgroundColor(backgroundColor)
        viewModel.searchFinishCallback = searchFinishCallback
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
                    AppConfig.searchGroup = ""
                } else {
                    AppConfig.searchGroup = item.title.toString()
                }
                searchView.query?.toString()?.trim()?.let {
                    searchView.setQuery(it, true)
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_key)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                query?.let {
                    isManualStopSearch = false
                    viewModel.saveSearchKey(query)
                    viewModel.searchKey = ""
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
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.rvBookshelfSearch.setEdgeEffectColor(primaryColor)
        binding.rvHistoryKey.setEdgeEffectColor(primaryColor)
        binding.rvBookshelfSearch.layoutManager = FlexboxLayoutManager(this)
        binding.rvBookshelfSearch.adapter = bookAdapter
        binding.rvHistoryKey.layoutManager = FlexboxLayoutManager(this)
        binding.rvHistoryKey.adapter = historyKeyAdapter
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
            isManualStopSearch = true
            viewModel.stop()
            binding.refreshProgressBar.isAutoLoading = false
        }
        binding.tvClearHistory.setOnClickListener { viewModel.clearHistory() }
    }

    private fun initData() {
        viewModel.isSearchLiveData.observe(this) {
            if (it) {
                startSearch()
            } else {
                searchFinally()
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.searchDataFlow.conflate().collect {
                adapter.setItems(it)
                delay(1000)
            }
        }
        launch {
            appDb.bookSourceDao.flowEnabledGroups().conflate().collect {
                groups.clear()
                groups.addAll(it)
                upGroupMenu()
            }
        }
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
        if (isManualStopSearch) {
            return
        }
        if (viewModel.isSearchLiveData.value == false
            && viewModel.searchKey.isNotEmpty()
            && loadMoreView.hasMore
        ) {
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
        val selectedGroup = AppConfig.searchGroup
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
                appDb.bookDao.flowSearch(key).conflate().collect {
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
                key.isNullOrBlank() -> appDb.searchKeywordDao.flowByTime()
                else -> appDb.searchKeywordDao.flowSearch(key)
            }.conflate().collect {
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
    override fun showBookInfo(name: String, author: String, bookUrl: String) {
        startActivity<BookInfoActivity> {
            putExtra("name", name)
            putExtra("author", author)
            putExtra("bookUrl", bookUrl)
        }
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(book: Book) {
        showBookInfo(book.name, book.author, book.bookUrl)
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