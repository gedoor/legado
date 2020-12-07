package io.legado.app.ui.book.search

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.databinding.ActivityBookSearchBinding
import io.legado.app.lib.theme.*
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity
import java.text.Collator


class SearchActivity : VMBaseActivity<ActivityBookSearchBinding, SearchViewModel>(),
    BookAdapter.CallBack,
    HistoryKeyAdapter.CallBack,
    SearchAdapter.CallBack {

    override val viewModel: SearchViewModel
        get() = getViewModel(SearchViewModel::class.java)

    lateinit var adapter: SearchAdapter
    private lateinit var bookAdapter: BookAdapter
    private lateinit var historyKeyAdapter: HistoryKeyAdapter
    private lateinit var loadMoreView: LoadMoreView
    private lateinit var serchView: SearchView
    private var historyData: LiveData<List<SearchKeyword>>? = null
    private var bookData: LiveData<List<Book>>? = null
    private var menu: Menu? = null
    private var precisionSearchMenuItem: MenuItem? = null
    private var groups = linkedSetOf<String>()

    override fun getViewBinding(): ActivityBookSearchBinding {
        return ActivityBookSearchBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.llHistory.setBackgroundColor(backgroundColor)
        serchView = binding.titleBar.findViewById(R.id.search_view)
        initRecyclerView()
        initSearchView()
        initOtherView()
        initLiveData()
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
                serchView.query?.toString()?.trim()?.let {
                    serchView.setQuery(it, true)
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
                serchView.query?.toString()?.trim()?.let {
                    serchView.setQuery(it, true)
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initSearchView() {
        ATH.setTint(serchView, primaryTextColor)
        serchView.onActionViewExpanded()
        serchView.isSubmitButtonEnabled = true
        serchView.queryHint = getString(R.string.search_book_key)
        serchView.clearFocus()
        serchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                serchView.clearFocus()
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
        serchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus && serchView.query.toString().trim().isEmpty()) {
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
        binding.fbStop.onClick {
            viewModel.stop()
            binding.refreshProgressBar.isAutoLoading = false
        }
        binding.tvClearHistory.onClick { viewModel.clearHistory() }
    }

    private fun initLiveData() {
        App.db.bookSourceDao.liveGroupEnabled().observe(this, {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
            }
            upGroupMenu()
        })
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
        intent?.getStringExtra("key")?.let {
            serchView.setQuery(it, true)
        } ?: let {
            serchView.requestFocus()
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
            upHistory(serchView.query.toString())
            binding.llHistory.visibility = VISIBLE
        } else {
            binding.llHistory.visibility = GONE
        }
    }

    /**
     * 更新分组菜单
     */
    private fun upGroupMenu() {
        val selectedGroup = getPrefString("searchGroup") ?: ""
        menu?.removeGroup(R.id.source_group)
        var item = menu?.add(R.id.source_group, Menu.NONE, Menu.NONE, R.string.all_source)
        if (selectedGroup == "") {
            item?.isChecked = true
        }
        groups.sortedWith(Collator.getInstance(java.util.Locale.CHINESE))
            .map {
                item = menu?.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
                if (it == selectedGroup) {
                    item?.isChecked = true
                }
            }
        menu?.setGroupCheckable(R.id.source_group, true, true)
    }

    /**
     * 更新搜索历史
     */
    private fun upHistory(key: String? = null) {
        bookData?.removeObservers(this)
        if (key.isNullOrBlank()) {
            binding.tvBookShow.gone()
            binding.rvBookshelfSearch.gone()
        } else {
            bookData = App.db.bookDao.liveDataSearch(key)
            bookData?.observe(this, {
                if (it.isEmpty()) {
                    binding.tvBookShow.gone()
                    binding.rvBookshelfSearch.gone()
                } else {
                    binding.tvBookShow.visible()
                    binding.rvBookshelfSearch.visible()
                }
                bookAdapter.setItems(it)
            })
        }
        historyData?.removeObservers(this)
        historyData =
            if (key.isNullOrBlank()) {
                App.db.searchKeywordDao.liveDataByUsage()
            } else {
                App.db.searchKeywordDao.liveDataSearch(key)
            }
        historyData?.observe(this, {
            historyKeyAdapter.setItems(it)
            if (it.isEmpty()) {
                binding.tvClearHistory.invisible()
            } else {
                binding.tvClearHistory.visible()
            }
        })
    }

    /**
     * 更新搜索结果
     */
    @Synchronized
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
        viewModel.getSearchBook(name, author) { searchBook ->
            searchBook?.let {
                startActivity<BookInfoActivity>(
                    Pair("name", it.name),
                    Pair("author", it.author)
                )
            }
        }
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(book: Book) {
        startActivity<BookInfoActivity>(
            Pair("name", book.name),
            Pair("author", book.author)
        )
    }

    /**
     * 点击历史关键字
     */
    override fun searchHistory(key: String) {
        launch {
            when {
                serchView.query.toString() == key -> {
                    serchView.setQuery(key, true)
                }
                withContext(IO) { App.db.bookDao.findByName(key).isEmpty() } -> {
                    serchView.setQuery(key, true)
                }
                else -> {
                    serchView.setQuery(key, false)
                }
            }
        }
    }
}