package io.legado.app.ui.main.explore

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.constant.BookSourceType
import com.google.android.material.tabs.TabLayout
import androidx.core.view.isVisible
import com.google.android.material.chip.Chip
import io.legado.app.data.entities.BookSource
import io.legado.app.help.source.exploreKinds
import io.legado.app.databinding.FragmentExploreBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.book.explore.ExploreShowActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.book.search.SearchScope
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.utils.applyTint
import io.legado.app.utils.flowWithLifecycleAndDatabaseChange
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.startActivity
import io.legado.app.utils.transaction
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 发现界面
 */
class ExploreFragment() : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore),
    MainFragmentInterface,
    ExploreAdapter.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    override val viewModel by viewModels<ExploreViewModel>()
    private val binding by viewBinding(FragmentExploreBinding::bind)
    private val adapter by lazy { ExploreAdapter(requireContext(), this) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val diffItemCallBack = ExploreDiffItemCallBack()
    private val groups = linkedSetOf<String>()
    private var exploreFlowJob: Job? = null
    private var groupsMenu: SubMenu? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
        initRecyclerView()
        initSourceTypeTabs()
        initCurrentSourceCard()
        initGroupChips()
        initCategoryChips()
        initGroupData()
        observeViewModel()
        upExploreData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        super.onCompatCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_explore, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
    }

    override fun onPause() {
        super.onPause()
        searchView.clearFocus()
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.screen_find)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                upExploreData(newText)
                return false
            }
        })
    }

    private fun initRecyclerView() {
        binding.rvFind.setEdgeEffectColor(primaryColor)
        binding.rvFind.layoutManager = linearLayoutManager
        binding.rvFind.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    binding.rvFind.scrollToPosition(0)
                }
            }
        })
    }

    // 新增：初始化书源类型切换标签
    private fun initSourceTypeTabs() {
        // 定义书源类型和对应的显示名称
        val sourceTypes = listOf(
            BookSourceType.default to "服务",
            BookSourceType.audio to "音频", 
            BookSourceType.image to "漫画",
            BookSourceType.file to "文件"
        )
        
        // 添加标签页
        sourceTypes.forEach { (type, name) ->
            binding.tabSourceType.addTab(
                binding.tabSourceType.newTab().setText(name)
            )
        }
        
        // 设置标签选择监听器
        binding.tabSourceType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val selectedType = sourceTypes[it.position].first
                    // 更新ViewModel中的选中类型
                    viewModel.setSourceType(selectedType)
                    // 刷新分组和分类
                    refreshGroupsAndCategories()
                    // 更新数据
                    upExploreData()
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // 新增：初始化当前书源信息卡片
    private fun initCurrentSourceCard() {
        binding.btnSwitchSource.setOnClickListener {
            showBookSourceSelector()
        }
    }

    // 新增：初始化分组筛选芯片
    private fun initGroupChips() {
        // 分组芯片的初始化在observeViewModel中处理
    }

    // 新增：初始化分类筛选芯片
    private fun initCategoryChips() {
        // 分类芯片的初始化在observeViewModel中处理
    }

    // 新增：观察ViewModel数据变化
    private fun observeViewModel() {
        viewModel.currentBookSource.observe(viewLifecycleOwner) { bookSource ->
            if (bookSource != null) {
                binding.cardCurrentSource.isVisible = true
                binding.tvCurrentSourceInfo.text = "当前书源：${viewModel.getCurrentSourceDisplayInfo()}"
                refreshGroupsAndCategories()
            } else {
                binding.cardCurrentSource.isGone = true
            }
        }
        
        viewModel.selectedGroup.observe(viewLifecycleOwner) { group ->
            refreshGroupChips()
            refreshCategoryChips()
        }
        
        viewModel.selectedCategory.observe(viewLifecycleOwner) { category ->
            refreshCategoryChips()
        }
    }

    private fun initGroupData() {
        viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookSourceDao.flowExploreGroups()
                .flowWithLifecycleAndDatabaseChange(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED,
                    AppDatabase.BOOK_SOURCE_TABLE_NAME
                )
                .conflate()
                .distinctUntilChanged()
                .collect {
                    groups.clear()
                    groups.addAll(it)
                    upGroupsMenu()
                    delay(500)
                }
        }
    }

    // 新增：显示书源选择器
    private fun showBookSourceSelector() {
        val currentType = viewModel.selectedSourceType.value ?: BookSourceType.default
        val availableSources = appDb.bookSourceDao.getEnabledByType(currentType)
            .filter { !it.exploreUrl.isNullOrBlank() && it.enabledExplore }
        
        if (availableSources.isEmpty()) {
            alert("提示", "当前类型下没有可用的书源") { }
            return
        }
        
        val sourceNames = availableSources.map { 
            if (it.bookSourceGroup.isNullOrBlank()) {
                it.bookSourceName
            } else {
                "${it.bookSourceName} (${it.bookSourceGroup})"
            }
        }
        
        alert("选择书源", "请选择要切换到的书源") {
            items(sourceNames) { _, index ->
                val selectedSource = availableSources[index]
                viewModel.setCurrentBookSource(selectedSource)
                upExploreData()
            }
        }
    }

    // 新增：刷新分组和分类
    private fun refreshGroupsAndCategories() {
        refreshGroupChips()
        refreshCategoryChips()
    }

    // 新增：刷新分组芯片
    private fun refreshGroupChips() {
        binding.chipGroupGroups.removeAllViews()
        
        val currentSource = viewModel.currentBookSource.value
        val availableGroups = if (currentSource != null) {
            // 如果选择了特定书源，显示该书源的分组
            currentSource.bookSourceGroup?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        } else {
            // 否则显示当前类型下所有可用的分组
            viewModel.getAvailableGroups()
        }
        
        if (availableGroups.isNotEmpty()) {
            binding.scrollGroups.isVisible = true
            
            // 添加"全部"选项
            val allChip = createChip("全部", null)
            binding.chipGroupGroups.addView(allChip)
            
            availableGroups.forEach { group ->
                val chip = createChip(group, group)
                binding.chipGroupGroups.addView(chip)
            }
            
            // 设置当前选中的分组
            val selectedGroup = viewModel.selectedGroup.value
            if (selectedGroup != null) {
                binding.chipGroupGroups.findViewWithTag<Chip>(selectedGroup)?.isChecked = true
            }
        } else {
            binding.scrollGroups.isGone = true
        }
    }

    // 新增：刷新分类芯片
    private fun refreshCategoryChips() {
        binding.chipGroupCategories.removeAllViews()
        
        val currentSource = viewModel.currentBookSource.value
        if (currentSource != null) {
            // 如果选择了特定书源，显示该书的分类
            binding.scrollCategories.isVisible = true
            
            // 这里需要异步获取分类，暂时显示加载中
            val loadingChip = createChip("加载中...", null)
            binding.chipGroupCategories.addView(loadingChip)
            
            // 异步获取分类
            lifecycleScope.launch {
                try {
                    val categories = currentSource.exploreKinds()
                    binding.chipGroupCategories.removeAllViews()
                    
                    if (categories.isNotEmpty()) {
                        // 添加"全部"选项
                        val allChip = createChip("全部", null)
                        binding.chipGroupCategories.addView(allChip)
                        
                        categories.forEach { category ->
                            val chip = createChip(category.title, category.title)
                            binding.chipGroupCategories.addView(chip)
                        }
                        
                        // 设置当前选中的分类
                        val selectedCategory = viewModel.selectedCategory.value
                        if (selectedCategory != null) {
                            binding.chipGroupCategories.findViewWithTag<Chip>(selectedCategory)?.isChecked = true
                        }
                    } else {
                        binding.scrollCategories.isGone = true
                    }
                } catch (e: Exception) {
                    binding.scrollCategories.isGone = true
                }
            }
        } else {
            binding.scrollCategories.isGone = true
        }
    }

    // 新增：创建芯片
    private fun createChip(text: String, tag: String?): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            this.tag = tag
            this.isCheckable = true
            this.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    when (tag) {
                        null -> {
                            // "全部"选项
                            try {
                                val parent = this.parent
                                if (parent == binding.chipGroupGroups) {
                                    viewModel.setGroup(null)
                                } else if (parent == binding.chipGroupCategories) {
                                    viewModel.setCategory(null)
                                }
                            } catch (e: Exception) {
                                // 忽略异常，避免崩溃
                            }
                        }
                        else -> {
                            try {
                                val parent = this.parent
                                if (parent == binding.chipGroupGroups) {
                                    viewModel.setGroup(tag)
                                } else if (parent == binding.chipGroupCategories) {
                                    viewModel.setCategory(tag)
                                }
                            } catch (e: Exception) {
                                // 忽略异常，避免崩溃
                            }
                        }
                    }
                    upExploreData()
                }
            }
        }
    }

    private fun upExploreData(searchKey: String? = null) {
        exploreFlowJob?.cancel()
        exploreFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            val filteredSources = viewModel.getFilteredBookSources()
            
            // 根据搜索关键词进一步筛选
            val finalSources = when {
                searchKey.isNullOrBlank() -> filteredSources
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    filteredSources.filter { source ->
                        source.bookSourceGroup?.contains(key) == true
                    }
                }
                else -> {
                    filteredSources.filter { source ->
                        source.bookSourceName.contains(searchKey, ignoreCase = true) ||
                        source.bookSourceGroup?.contains(searchKey, ignoreCase = true) == true
                    }
                }
            }
            
            binding.tvEmptyMsg.isGone = finalSources.isNotEmpty() || searchView.query.isNotEmpty()
            adapter.setItems(finalSources, diffItemCallBack)
            delay(500)
        }
    }

    private fun upGroupsMenu() = groupsMenu?.transaction { subMenu ->
        subMenu.removeGroup(R.id.menu_group_text)
        groups.forEach {
            subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
        }
    }

    override val scope: CoroutineScope
        get() = viewLifecycleOwner.lifecycleScope

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        if (item.groupId == R.id.menu_group_text) {
            searchView.setQuery("group:${item.title}", true)
        }
    }

    override fun scrollTo(pos: Int) {
        (binding.rvFind.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
    }

    override fun openExplore(sourceUrl: String, title: String, exploreUrl: String?) {
        if (exploreUrl.isNullOrBlank()) return
        startActivity<ExploreShowActivity> {
            putExtra("exploreName", title)
            putExtra("sourceUrl", sourceUrl)
            putExtra("exploreUrl", exploreUrl)
        }
    }

    override fun editSource(sourceUrl: String) {
        startActivity<BookSourceEditActivity> {
            putExtra("sourceUrl", sourceUrl)
        }
    }

    override fun toTop(source: BookSourcePart) {
        viewModel.topSource(source)
    }

    override fun deleteSource(source: BookSourcePart) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + source.bookSourceName)
            noButton()
            yesButton {
                viewModel.deleteSource(source)
            }
        }
    }

    override fun searchBook(bookSource: BookSourcePart) {
        startActivity<SearchActivity> {
            putExtra("searchScope", SearchScope(bookSource).toString())
        }
    }

    fun compressExplore() {
        if (!adapter.compressExplore()) {
            if (AppConfig.isEInkMode) {
                binding.rvFind.scrollToPosition(0)
            } else {
                binding.rvFind.smoothScrollToPosition(0)
            }
        }
    }

}
