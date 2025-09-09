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
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.model.webBook.WebBook
import io.legado.app.databinding.FragmentExploreBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.book.explore.ExploreShowActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.book.search.SearchScope
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.data.entities.SearchBook
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.utils.applyTint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers.IO
import android.content.Context
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.app.AlertDialog

/**
 * 发现界面
 */
class ExploreFragment() : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore),
    MainFragmentInterface,
    ExploreAdapter.CallBack,
    SourceSelectorDialog.CallBack {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    override val viewModel by viewModels<ExploreViewModel>()
    private val binding by viewBinding(FragmentExploreBinding::bind)
    private val adapter by lazy { ExploreAdapter(requireContext(), this) }
    private val bookGridAdapter by lazy { BookGridAdapter(requireContext(), object : BookGridAdapter.CallBack {
                           override fun onBookClick(book: SearchBook) {
              // 添加详细的调试日志
              AppLog.put("书籍点击事件触发: ${book.name} - ${book.author}")
              AppLog.put("书籍URL: ${book.bookUrl}")
              AppLog.put("书籍信息: name=${book.name}, author=${book.author}, intro=${book.intro?.take(50)}")
              AppLog.put("书籍数据完整性: name=${!book.name.isBlank()}, author=${!book.author.isBlank()}, bookUrl=${!book.bookUrl.isBlank()}")
              
              // 获取当前书源类型，为动漫类书源提供更宽松的验证
              val currentSource = viewModel.currentBookSource.value
              val isAnimeSource = currentSource?.bookSourceType == BookSourceType.image
              
              AppLog.put("当前书源类型: ${currentSource?.bookSourceType}, 是否为动漫类: $isAnimeSource")
              
              // 数据完整性验证（动漫类书源放宽标准）
              val isValidBook = if (isAnimeSource) {
                  // 动漫类书源：只要求书名和URL不为空
                  !book.name.isBlank() && !book.bookUrl.isBlank()
              } else {
                  // 其他类型书源：要求书名、作者、URL都不为空
                  !book.name.isBlank() && !book.author.isBlank() && !book.bookUrl.isBlank()
              }
              
              if (!isValidBook) {
                  val requiredFields = if (isAnimeSource) "书名和URL" else "书名、作者和URL"
                  AppLog.put("警告: 书籍数据不完整，无法打开详情页。需要字段: $requiredFields")
                  android.widget.Toast.makeText(requireContext(), "书籍信息不完整，请重试", android.widget.Toast.LENGTH_SHORT).show()
                  return
              }
             
             // 数据完整，直接打开详情页
             openBookDetail(book)
         }
         
         override fun onBookLongClick(book: SearchBook) {
             // 显示长按菜单
             showBookLongPressMenu(book)
         }
         
                                       
     }) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context) }
    private var searchView: SearchView? = null
    private val diffItemCallBack = ExploreDiffItemCallBack()
    private var exploreFlowJob: Job? = null
    private var searchJob: Job? = null
    private var bookLoadingJob: Job? = null
    private var isBookLoading = false
    private var lastCategoryClickTime = 0L
    private val categoryClickDebounceTime = 1000L // 1秒防抖

         override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
         // 立即初始化基本UI，避免阻塞
         initBasicUI()
         
         // 异步初始化其他组件
         lifecycleScope.launch {
             initAsyncComponents()
         }
     }
     
           override fun onResume() {
          super.onResume()
          // 每次回到发现界面时，根据当前选中的分类显示有限数量的书籍
          lifecycleScope.launch {
              try {
                  val currentSource = viewModel.currentBookSource.value
                  val currentCategory = viewModel.selectedCategory.value
                  
                  if (currentSource != null && binding.chipGroupCategories.childCount > 0) {
                      AppLog.put("onResume: 当前分类: $currentCategory")
                      
                      // 延迟一下，避免与初始化冲突
                      delay(500)
                      
                                           // 修复：用户返回发现界面时，保持分类状态，避免自动重置
                     if (currentCategory.isNullOrBlank()) {
                         // 只有在真正没有分类时才加载默认书籍
                         AppLog.put("onResume: 没有选中分类，加载默认书籍")
                         loadAndShowBooks()
                     } else if (currentCategory == "refresh") {
                         // 刷新时保持当前分类，不重置
                         val lastCategory = viewModel.lastSelectedCategory.value
                         if (lastCategory.isNullOrBlank()) {
                             AppLog.put("onResume: 刷新模式，加载默认书籍")
                             loadAndShowBooks()
                         } else {
                             AppLog.put("onResume: 刷新模式，保持分类: $lastCategory")
                             // 修复：刷新时先清除旧数据，避免显示错误内容
                             clearBooksData()
                             loadBooksWithPreload(lastCategory, showLimited = true)
                         }
                     } else {
                         // 正常分类加载，保存当前分类用于刷新时恢复
                         AppLog.put("onResume: 加载分类书籍: $currentCategory")
                         viewModel.lastSelectedCategory.value = currentCategory
                         // 修复：切换分类时先清除旧数据
                         clearBooksData()
                         loadBooksWithPreload(currentCategory, showLimited = true)
                     }
                  }
              } catch (e: Exception) {
                  AppLog.put("onResume 刷新书籍数据失败", e)
              }
          }
      }
    
    // 新增：初始化基本UI（同步，快速）
    private fun initBasicUI() {
        try {
            // 初始化 searchView
            searchView = binding.titleBar.findViewById(R.id.search_view)
            if (searchView != null) {
                initSearchView()
                AppLog.put("搜索视图初始化成功")
            } else {
                AppLog.put("警告: 搜索视图未找到")
            }
            
            // 初始化 RecyclerView
            initRecyclerView()
            
            // 设置基本状态
            binding.cardCurrentSource.isVisible = true
            binding.tvCurrentSourceInfo.text = "当前书源：未选择，点击右侧切换"
            
            // 初始化书源类型标签页（轻量级操作）
            initSourceTypeTabs()
            
            // 初始化新按钮
            initNewButtons()
            
            AppLog.put("基本UI初始化完成")
        } catch (e: Exception) {
            AppLog.put("基本UI初始化失败", e)
        }
    }
    
    // 新增：异步初始化组件
    private suspend fun initAsyncComponents() {
        try {
            // 使用 withContext 确保在正确的线程上执行
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                // 初始化当前书源卡片
                initCurrentSourceCard()
                
                // 观察ViewModel数据变化
                observeViewModel()
                
                // 初始化书籍展示区域
                initBooksSection()
            }
            
                         // 在IO线程上执行数据库操作
             withContext(kotlinx.coroutines.Dispatchers.IO) {
                 // 确保有默认书源
                 viewModel.ensureDefaultCurrentSource()
             }
            
            // 加载并显示书籍
            loadAndShowBooks()
            
        } catch (e: Exception) {
            // 记录错误但不崩溃
            AppLog.put("初始化组件失败", e)
        }
    }
    
    // 新增：清除书籍数据
    private fun clearBooksData() {
        try {
            binding.cardBooksSection.isVisible = false
            bookGridAdapter.setItems(emptyList())
            AppLog.put("分类切换：清除书籍数据")
        } catch (e: Exception) {
            AppLog.put("清除书籍数据失败", e)
        }
    }
    
    // 新增：预加载书籍详情，避免返回空页面
    private fun preloadBookDetails(books: List<SearchBook>) {
        try {
            AppLog.put("开始预加载 ${books.size} 本书的详情")
            
            // 异步预加载书籍详情
            lifecycleScope.launch(IO) {
                books.forEach { book ->
                    try {
                        // 预加载书籍详情，确保返回时有数据
                        if (book.bookUrl.isNotBlank()) {
                            // 这里可以调用预加载逻辑，暂时记录日志
                            AppLog.put("预加载书籍: ${book.name} - ${book.bookUrl}")
                        }
                    } catch (e: Exception) {
                        AppLog.put("预加载书籍失败: ${book.name}", e)
                    }
                }
                AppLog.put("预加载完成")
            }
        } catch (e: Exception) {
            AppLog.put("预加载书籍详情失败", e)
        }
    }
    
    // 新增：防抖搜索
    private fun debouncedSearch(query: String?) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300) // 300ms 防抖延迟
            if (query.isNullOrBlank()) {
                // 搜索为空时，显示默认书籍
                loadAndShowBooks()
            } else {
                // 执行搜索，结果显示在书籍网格中
                performSearch(query)
            }
        }
    }
    
    // 新增：执行搜索功能
    private suspend fun performSearch(query: String) {
        try {
            val currentSource = viewModel.currentBookSource.value
            if (currentSource == null) {
                AppLog.put("当前书源为空，无法执行搜索")
                return
            }
            
            AppLog.put("开始搜索: $query")
            
            // 显示搜索状态
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                binding.cardBooksSection.isVisible = true
                binding.tvBooksSectionTitle.text = "搜索: $query"
                bookGridAdapter.setItems(emptyList())
            }
            
            // 在IO线程执行搜索
            val searchResults = withContext(IO) {
                try {
                    // 使用WebBook搜索功能
                    WebBook.searchBook(lifecycleScope, currentSource, query)
                        .timeout(30000L)
                        .onSuccess(IO) { books ->
                            AppLog.put("搜索成功，结果数量: ${books.size}")
                            // 在主线程更新UI
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                 if (books.isNotEmpty()) {
                                     binding.cardBooksSection.isVisible = true
                                     binding.tvBooksSectionTitle.text = "搜索结果: $query (${books.size}本)"
                                     // 显示全部搜索结果，不再限制数量
                                     bookGridAdapter.setItems(books)
                                     AppLog.put("搜索结果显示完成")
                                 } else {
                                    binding.cardBooksSection.isVisible = true
                                    binding.tvBooksSectionTitle.text = "搜索: $query (无结果)"
                                    bookGridAdapter.setItems(emptyList())
                                    AppLog.put("搜索无结果")
                                }
                            }
                        }.onError { e ->
                            AppLog.put("搜索失败", e)
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                binding.cardBooksSection.isVisible = true
                                binding.tvBooksSectionTitle.text = "搜索失败: $query"
                                bookGridAdapter.setItems(emptyList())
                            }
                        }
                } catch (e: Exception) {
                    AppLog.put("搜索异常", e)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.cardBooksSection.isVisible = true
                        binding.tvBooksSectionTitle.text = "搜索异常: $query"
                        bookGridAdapter.setItems(emptyList())
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.put("搜索执行失败", e)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        super.onCompatCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_explore, menu)
    }

    override fun onPause() {
        super.onPause()
        searchView?.clearFocus()
    }

    private fun initSearchView() {
        searchView?.let { view ->
            try {
                view.applyTint(primaryTextColor)
                view.isSubmitButtonEnabled = true
                view.queryHint = getString(R.string.screen_find)
                view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        AppLog.put("搜索提交: $query")
                        debouncedSearch(query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (!newText.isNullOrBlank()) {
                            AppLog.put("搜索输入: $newText")
                            debouncedSearch(newText)
                        }
                        return true
                    }
                })
                AppLog.put("搜索视图监听器设置成功")
            } catch (e: Exception) {
                AppLog.put("搜索视图初始化失败", e)
            }
        } ?: run {
            AppLog.put("错误: 搜索视图为空")
        }
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

         // 新增：初始化书源类型切换标签（增强版）
     private fun initSourceTypeTabs() {
         // 定义书源类型和对应的显示名称
         val sourceTypes = listOf(
             BookSourceType.default to "小说",
             BookSourceType.audio to "音频", 
             BookSourceType.image to "漫画",
             BookSourceType.file to "电影"
         )
         
         // 批量添加标签页，减少重绘
         sourceTypes.forEach { (type, name) ->
             binding.tabSourceType.addTab(
                 binding.tabSourceType.newTab().setText(name)
             )
         }
         
         // 设置标签选择监听器（延迟执行，避免阻塞）
         binding.tabSourceType.post {
             binding.tabSourceType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                 override fun onTabSelected(tab: TabLayout.Tab?) {
                     tab?.let {
                         val selectedType = sourceTypes[it.position].first
                         val selectedName = sourceTypes[it.position].second
                         AppLog.put("书源类型切换: $selectedType -> $selectedName")
                         
                         // 异步更新，避免阻塞UI
                         lifecycleScope.launch {
                             try {
                                 // 先设置书源类型
                                 viewModel.setSourceType(selectedType)
                                 AppLog.put("书源类型已设置: $selectedType")
                                 
                                 // 延迟执行，避免频繁刷新
                                 delay(100)
                                 
                                 // 确保有默认书源
                                 viewModel.ensureDefaultCurrentSource()
                                 AppLog.put("默认书源已确保")
                                 
                                 // 等待书源设置完成
                                 delay(200)
                                 
                                 // 重新加载分类
                                 refreshCategories()
                                 AppLog.put("分类已刷新")
                                 
                                 // 等待分类加载完成
                                 delay(300)
                                 
                                 // 加载新书源类型的书籍（增强版）
                                 loadAndShowBooksEnhanced()
                                 AppLog.put("书籍加载已启动")
                             } catch (e: Exception) {
                                 AppLog.put("书源类型切换失败", e)
                                 // 显示错误提示
                                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                                     android.widget.Toast.makeText(requireContext(), "切换书源类型失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                 }
                             }
                         }
                     }
                 }
                 
                 override fun onTabUnselected(tab: TabLayout.Tab?) {}
                 override fun onTabReselected(tab: TabLayout.Tab?) {}
             })
         }
     }

    // 新增：初始化当前书源信息卡片
    private fun initCurrentSourceCard() {
        binding.btnSwitchSource.setOnClickListener {
            showBookSourceSelector()
        }
    }

    private fun formatRelativeTime(time: Long): String {
        // 简易相对时间显示，避免引入新依赖
        val now = System.currentTimeMillis()
        val diff = (now - time).coerceAtLeast(0)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        return when {
            diff < minute -> "刚刚"
            diff < hour -> "${diff / minute} 分钟前"
            diff < day -> "${diff / hour} 小时前"
            diff < 7 * day -> "${diff / day} 天前"
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
        }
    }



    // 新增：初始化分类筛选芯片
    private fun initCategoryChips() {
        // 分类芯片的初始化在observeViewModel中处理
    }

    // 新增：观察ViewModel数据变化
    private fun observeViewModel() {
        viewModel.currentBookSource.observe(viewLifecycleOwner) { bookSource ->
            binding.cardCurrentSource.isVisible = true
            if (bookSource != null) {
                binding.tvCurrentSourceInfo.text = "当前书源：${viewModel.getCurrentSourceDisplayInfo()}"
                val needLogin = !bookSource.loginUrl.isNullOrBlank()
                val meta = buildString {
                    if (needLogin) append("需登录 · ")
                    append("响应 ")
                    append((bookSource.respondTime).coerceAtLeast(0))
                    append("ms")
                    if (bookSource.lastUpdateTime > 0) {
                        append(" · 最近更新 ")
                        append(formatRelativeTime(bookSource.lastUpdateTime))
                    }
                }
                binding.tvSourceMeta.text = meta
                
                                 // 只在书源变化时刷新分类，避免循环调用
                 if (binding.chipGroupCategories.childCount == 0) {
                     refreshCategories()
                 }
            } else {
                binding.tvCurrentSourceInfo.text = "当前书源：未选择，点击右侧切换"
                binding.tvSourceMeta.text = ""
            }
        }
        
        // 移除这些观察者，避免循环调用
        // viewModel.selectedGroup.observe(viewLifecycleOwner) { group ->
        //     refreshGroupChips()
        //     refreshCategoryChips()
        // }
        
        // viewModel.selectedCategory.observe(viewLifecycleOwner) { category ->
        //     refreshCategoryChips()
        // }
    }



    // 新增：显示书源选择器
    private fun showBookSourceSelector() {
        val dlg = SourceSelectorDialog()
        dlg.arguments = Bundle().apply {
            putInt("type", viewModel.selectedSourceType.value ?: BookSourceType.default)
        }
        dlg.show(childFragmentManager, "source_selector")
    }

    override fun onSourceSelected(source: io.legado.app.data.entities.BookSource) {
        AppLog.put("书源选择: ${source.bookSourceName}")
        viewModel.setCurrentBookSource(source)
                 // 书源变化后，重新加载分类
         lifecycleScope.launch {
             try {
                 // 延迟一下，确保书源设置完成
                 delay(100)
                 refreshCategories()
                 // 加载新书源的书籍
                 loadAndShowBooks()
                 AppLog.put("书源切换完成，数据已刷新")
             } catch (e: Exception) {
                 AppLog.put("书源切换后刷新失败", e)
             }
         }
    }

    // 新增：刷新分类
    private fun refreshCategories() {
        refreshCategoryChips()
    }



    // 新增：刷新分类芯片（优化版）
    private fun refreshCategoryChips() {
        val currentSource = viewModel.currentBookSource.value
        if (currentSource != null) {
            // 立即显示加载状态
            binding.scrollCategories.isVisible = true
            binding.chipGroupCategories.removeAllViews()
            
            val loadingChip = createChip("加载中...", null)
            binding.chipGroupCategories.addView(loadingChip)
            
            // 异步获取分类
            lifecycleScope.launch {
                try {
                    val categories = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        currentSource.exploreKinds()
                    }
                    
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.chipGroupCategories.removeAllViews()
                        
                        if (categories.isNotEmpty()) {
                            AppLog.put("成功加载分类，数量: ${categories.size}")
                            categories.forEach { AppLog.put("分类: ${it.title} - URL: ${it.url}") }
                            
                                                    binding.btnMoreCategories.isVisible = categories.size > 5
                        // 隐藏换一换按钮，因为功能已整合到第一个分类
                        binding.btnRefreshCategories.isVisible = false
                        
                        // 批量创建芯片，第一个位置放"换一换"功能
                        val chips = mutableListOf<Chip>()
                        // 第一个位置：换一换功能（不显示"全部"文字）
                        chips.add(createChip("换一换", "refresh"))
                            
                            val displayCategories = categories.take(5)
                            displayCategories.forEach { category ->
                                chips.add(createChip(category.title, category.title))
                                AppLog.put("添加分类芯片: ${category.title}")
                            }
                            
                            // 一次性添加所有芯片
                            chips.forEach { binding.chipGroupCategories.addView(it) }
                            AppLog.put("芯片添加完成，总数: ${binding.chipGroupCategories.childCount}")
                            
                                                    // 默认选中第一个"换一换"位置
                        binding.chipGroupCategories.findViewWithTag<Chip>("refresh")?.isChecked = true
                        } else {
                            AppLog.put("没有可用的分类")
                            binding.scrollCategories.isGone = true
                            binding.btnMoreCategories.isGone = true
                            binding.btnRefreshCategories.isGone = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.scrollCategories.isGone = true
                        binding.btnMoreCategories.isGone = true
                        binding.btnRefreshCategories.isGone = true
                    }
                    AppLog.put("刷新分类芯片失败", e)
                }
            }
        } else {
            binding.scrollCategories.isGone = true
            binding.btnMoreCategories.isGone = true
            binding.btnRefreshCategories.isGone = true
        }
    }

    // 新增：创建芯片
    private fun createChip(text: String, tag: String?): Chip {
        return Chip(requireContext()).apply {
            this.text = text
            this.tag = tag
            this.isCheckable = true
            this.chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), android.R.color.transparent)
            )
            this.setTextColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.primaryText)
            ))
            this.checkedIcon = null
            this.setChipStrokeColorResource(R.color.primary)
            this.chipStrokeWidth = 1f
            
             this.setOnCheckedChangeListener { _, isChecked ->
                 if (isChecked) {
                     when (tag) {
                         "refresh" -> {
                             // "换一换"功能
                             try {
                                 AppLog.put("换一换芯片点击")
                                 viewModel.setCategory(null)
                                 // 智能预加载：随机选择分类并进入完整书籍列表预加载数据
                                 openRandomCategoryFullList()
                             } catch (e: Exception) {
                                 AppLog.put("换一换芯片点击处理失败", e)
                             }
                         }
                                                                            else -> {
                               try {
                                   AppLog.put("分类芯片点击: $tag")
                                   
                                   // 先设置分类状态
                                   viewModel.setCategory(tag)
                                   
                                   // 智能预加载：先进入完整书籍列表，返回后显示有限书籍
                                   tag?.let { categoryName ->
                                       // 首次点击分类时，直接进入完整列表预加载数据
                                       openCategoryFullList(categoryName)
                                   }
                               } catch (e: Exception) {
                                   AppLog.put("分类芯片点击处理失败", e)
                               }
                           }
                     }
                 }
             }
         }
     }

    private fun upExploreData(searchKey: String? = null) {
        exploreFlowJob?.cancel()
        exploreFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 在IO线程上执行数据筛选
                val finalSources = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val filteredSources = viewModel.getFilteredBookSources()
                    
                    // 根据搜索关键词进一步筛选
                    when {
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
                }
                
                // 在主线程上更新UI
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.tvEmptyMsg.isGone = finalSources.isNotEmpty() || !searchView?.query.isNullOrEmpty()
                    adapter.setItems(finalSources, diffItemCallBack)
                }
                
                // 添加防抖延迟，避免频繁刷新
                delay(100)
            } catch (e: Exception) {
                AppLog.put("更新探索数据失败", e)
                // 在主线程上显示错误状态
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.tvEmptyMsg.isVisible = true
                    binding.tvEmptyMsg.text = "加载失败，请重试"
                }
            }
        }
    }



    override val scope: CoroutineScope
        get() = viewLifecycleOwner.lifecycleScope

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
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

    // 新增：初始化新按钮
    private fun initNewButtons() {
        // 更多分类按钮
        binding.btnMoreCategories.setOnClickListener {
            showMoreCategoriesDialog()
        }
        
        // 换一换按钮
        binding.btnRefreshCategories.setOnClickListener {
            refreshCategoriesRandomly()
        }
    }



         // 新增：显示更多分类对话框（修复版）
     private fun showMoreCategoriesDialog() {
         val currentSource = viewModel.currentBookSource.value
         if (currentSource != null) {
             lifecycleScope.launch {
                 try {
                     val categories = withContext(kotlinx.coroutines.Dispatchers.IO) {
                         currentSource.exploreKinds()
                     }
                     if (categories.isNotEmpty()) {
                         val items = arrayOf("全部") + categories.map { it.title }.toTypedArray()
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             AlertDialog.Builder(requireContext())
                                 .setTitle("选择分类")
                                 .setItems(items) { _, which ->
                                     if (which == 0) {
                                         // 选择"全部"
                                         AppLog.put("更多分类对话框选择: 全部")
                                         viewModel.setCategory(null)
                                         // 加载默认书籍
                                         loadAndShowBooks()
                                                                           } else {
                                          // 选择具体分类
                                          val selectedCategory = categories[which - 1]
                                          AppLog.put("更多分类对话框选择: ${selectedCategory.title}")
                                          
                                          // 重要：先设置分类状态，再智能预加载
                                          viewModel.setCategory(selectedCategory.title)
                                          
                                          // 智能预加载：先进入完整书籍列表，返回后显示有限书籍
                                          openCategoryFullList(selectedCategory.title)
                                          
                                          // 更新分类芯片的选中状态
                                          updateCategoryChipSelection(selectedCategory.title)
                                      }
                                     // 关闭对话框
                                 }
                                 .show()
                         }
                     }
                 } catch (e: Exception) {
                     AppLog.put("显示更多分类对话框失败", e)
                 }
             }
         }
     }

         // 新增：随机刷新分类（修复版）
     private fun refreshCategoriesRandomly() {
         viewModel.currentBookSource.value?.let { currentSource ->
             lifecycleScope.launch {
                 try {
                     val newCategories = withContext(kotlinx.coroutines.Dispatchers.IO) {
                         currentSource.exploreKinds()
                     }
                                           if (newCategories.isNotEmpty()) {
                          val randomCategory = newCategories.random()
                          
                          // 先设置分类状态
                          viewModel.setCategory(randomCategory.title)
                          
                          // 智能预加载：先进入完整书籍列表，返回后显示有限书籍
                          openCategoryFullList(randomCategory.title)
                          
                          // 更新分类芯片的选中状态
                          updateCategoryChipSelection(randomCategory.title)
                          
                          AppLog.put("随机刷新分类完成: ${randomCategory.title}")
                      }
                 } catch (e: Exception) {
                     AppLog.put("随机刷新分类失败", e)
                     // 显示错误提示
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         android.widget.Toast.makeText(requireContext(), "刷新失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                     }
                 }
             }
         }
     }
    
    // 新增：随机加载分类书籍
    private fun loadAndShowBooksRandomly(category: ExploreKind) {
        // 防止重复调用
        if (isBookLoading) {
            AppLog.put("书籍正在加载中，跳过重复调用: loadAndShowBooksRandomly")
            return
        }
        
        bookLoadingJob?.cancel()
        bookLoadingJob = lifecycleScope.launch {
            try {
                isBookLoading = true
                AppLog.put("开始随机加载分类书籍: ${category.title}")
                
                val currentSource = viewModel.currentBookSource.value
                if (currentSource == null) {
                    AppLog.put("当前书源为空，无法加载书籍")
                    return@launch
                }
                
                // 显示加载状态
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.cardBooksSection.isVisible = true
                    binding.tvBooksSectionTitle.text = "加载中..."
                    bookGridAdapter.setItems(emptyList())
                }
                
                // 直接调用探索API获取该分类下的真实书籍
                if (!category.url.isNullOrBlank()) {
                    AppLog.put("随机分类URL: ${category.url}")
                    WebBook.exploreBook(lifecycleScope, currentSource, category.url)
                        .timeout(30000L)
                        .onSuccess(IO) { searchBooks ->
                            AppLog.put("成功获取随机书籍，数量: ${searchBooks.size}")
                            // 在主线程更新UI
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                 if (searchBooks.isNotEmpty()) {
                                     binding.cardBooksSection.isVisible = true
                                     binding.tvBooksSectionTitle.text = "${category.title} - 热门书籍 (${searchBooks.size}本)"
                                     // 显示更多书籍，支持滚动
                                     val displayBooks = if (searchBooks.size > 12) searchBooks.take(12) else searchBooks
                                     bookGridAdapter.setItems(displayBooks)
                                     AppLog.put("随机书籍列表更新完成，显示 ${displayBooks.size} 本，总共 ${searchBooks.size} 本")
                                 } else {
                                    binding.cardBooksSection.isVisible = false
                                    AppLog.put("随机分类下没有书籍")
                                }
                            }
                        }.onError { e ->
                            AppLog.put("随机加载书籍失败", e)
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                binding.cardBooksSection.isVisible = false
                            }
                        }
                } else {
                    AppLog.put("随机分类URL为空")
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.cardBooksSection.isVisible = false
                    }
                }
            } catch (e: Exception) {
                AppLog.put("随机加载书籍异常", e)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.cardBooksSection.isVisible = false
                }
            } finally {
                isBookLoading = false
                AppLog.put("随机书籍加载完成")
            }
        }
    }
    
    // 新增：初始化书籍展示区域
    private fun initBooksSection() {
        binding.rvBooksGrid.adapter = bookGridAdapter
        binding.btnViewAllBooks.setOnClickListener {
            // 跳转到完整的书籍列表
            viewModel.currentBookSource.value?.let { source ->
                                try {
                    val currentCategory = viewModel.selectedCategory.value
                    val categoryName = if (currentCategory.isNullOrBlank() || currentCategory == "refresh") "热门书籍" else currentCategory
                    AppLog.put("点击查看全部书籍，书源: ${source.bookSourceName}，分类: $categoryName")
                    
                    // 根据当前选中的分类构建探索URL
                    if (currentCategory.isNullOrBlank() || currentCategory == "refresh") {
                        // 查看全部书籍，使用默认探索URL
                        if (!source.exploreUrl.isNullOrBlank()) {
                            startActivity<ExploreShowActivity> {
                                putExtra("sourceUrl", source.bookSourceUrl)
                                putExtra("exploreUrl", source.exploreUrl)
                                putExtra("exploreName", "热门书籍")
                            }
                            AppLog.put("成功启动热门书籍列表")
                        } else {
                            AppLog.put("书源没有探索URL，无法查看全部书籍")
                            android.widget.Toast.makeText(requireContext(), "该书源暂不支持探索功能", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // 查看特定分类的书籍
                        lifecycleScope.launch {
                            try {
                                val categories = withContext(IO) {
                                    source.exploreKinds()
                                }
                                
                                if (categories.isEmpty()) {
                                    AppLog.put("书源没有可用分类")
                                    android.widget.Toast.makeText(requireContext(), "该书源暂没有可用分类", android.widget.Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                
                                val targetCategory = categories.find { it.title == currentCategory }
                                if (targetCategory != null && !targetCategory.url.isNullOrBlank()) {
                                    startActivity<ExploreShowActivity> {
                                        putExtra("sourceUrl", source.bookSourceUrl)
                                        putExtra("exploreUrl", targetCategory.url)
                                        putExtra("exploreName", "$currentCategory - 全部书籍")
                                    }
                                    AppLog.put("成功启动分类书籍列表: $currentCategory")
                                } else {
                                    // 如果找不到分类URL，尝试使用默认探索
                                    if (!source.exploreUrl.isNullOrBlank()) {
                                        startActivity<ExploreShowActivity> {
                                            putExtra("sourceUrl", source.bookSourceUrl)
                                            putExtra("exploreUrl", source.exploreUrl)
                                            putExtra("exploreName", "$currentCategory - 全部书籍")
                                        }
                                        AppLog.put("使用默认探索URL启动分类书籍列表: $currentCategory")
                                    } else {
                                        AppLog.put("书源没有探索URL，无法查看分类书籍")
                                        android.widget.Toast.makeText(requireContext(), "该书源暂不支持探索功能", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                AppLog.put("获取分类URL失败", e)
                                // 尝试使用默认探索
                                if (!source.exploreUrl.isNullOrBlank()) {
                                    startActivity<ExploreShowActivity> {
                                        putExtra("sourceUrl", source.bookSourceUrl)
                                        putExtra("exploreUrl", source.exploreUrl)
                                        putExtra("exploreName", "$currentCategory - 全部书籍")
                                    }
                                    AppLog.put("异常情况下使用默认探索URL")
                                } else {
                                    android.widget.Toast.makeText(requireContext(), "获取分类信息失败", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLog.put("启动 ExploreShowActivity 失败", e)
                    android.widget.Toast.makeText(requireContext(), "打开书籍列表失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                AppLog.put("当前书源为空，无法查看全部书籍")
                android.widget.Toast.makeText(requireContext(), "请先选择书源", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
         // 新增：加载并显示书籍（优化版）
     private fun loadAndShowBooks() {
         // 防止重复调用
         if (isBookLoading) {
             AppLog.put("书籍正在加载中，跳过重复调用: loadAndShowBooks")
             return
         }
         
         bookLoadingJob?.cancel()
         bookLoadingJob = lifecycleScope.launch {
             try {
                 isBookLoading = true
                 AppLog.put("开始加载默认书籍")
                 
                 val currentSource = viewModel.currentBookSource.value
                 if (currentSource == null) {
                     AppLog.put("当前书源为空，无法加载书籍")
                     return@launch
                 }
                 
                 // 显示加载状态
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     binding.cardBooksSection.isVisible = true
                     binding.tvBooksSectionTitle.text = "加载中..."
                     bookGridAdapter.setItems(emptyList())
                 }
                 
                 // 在IO线程获取分类列表
                 val categories = withContext(IO) {
                     try {
                         currentSource.exploreKinds()
                     } catch (e: Exception) {
                         AppLog.put("获取分类列表失败", e)
                         emptyList()
                     }
                 }
                 
                                   if (categories.isNotEmpty()) {
                      // 选择第一个分类作为默认显示
                      val firstCategory = categories.first()
                      if (!firstCategory.url.isNullOrBlank()) {
                          AppLog.put("使用默认分类: ${firstCategory.title}, URL: ${firstCategory.url}")
                          // 调用探索API获取该分类下的真实书籍
                          WebBook.exploreBook(lifecycleScope, currentSource, firstCategory.url)
                              .timeout(30000L)
                              .onSuccess(IO) { searchBooks ->
                                  AppLog.put("成功获取默认书籍，数量: ${searchBooks.size}")
                                  
                                                                    // 验证书籍数据的完整性
                                   val validBooks = searchBooks.filter { book ->
                                       !book.name.isBlank() && 
                                       !book.author.isBlank() && 
                                       !book.bookUrl.isBlank()
                                   }
                                   
                                   AppLog.put("数据完整性检查：总书籍 ${searchBooks.size}，有效书籍 ${validBooks.size}")
                                  
                                  // 在主线程更新UI，显示有限数量的书籍
                                  withContext(kotlinx.coroutines.Dispatchers.Main) {
                                      if (validBooks.isNotEmpty()) {
                                          binding.cardBooksSection.isVisible = true
                                          binding.tvBooksSectionTitle.text = "${firstCategory.title} - 热门书籍 (${validBooks.size}本)"
                                                                                    // 显示9-12本书，支持滚动查看更多
                                           val displayBooks = if (validBooks.size > 12) validBooks.take(12) else validBooks
                                           bookGridAdapter.setItems(displayBooks)
                                           AppLog.put("默认书籍列表更新完成，显示 ${displayBooks.size} 本，总共 ${validBooks.size} 本")
                                          
                                          // 记录第一本书的详细信息用于调试
                                          if (displayBooks.isNotEmpty()) {
                                              val firstBook = displayBooks.first()
                                              AppLog.put("第一本书详情: name=${firstBook.name}, author=${firstBook.author}, bookUrl=${firstBook.bookUrl}, intro=${firstBook.intro?.take(50)}")
                                          }
                                      } else {
                                          binding.cardBooksSection.isVisible = false
                                          AppLog.put("默认分类下没有有效书籍")
                                          android.widget.Toast.makeText(requireContext(), "该分类下暂无有效书籍", android.widget.Toast.LENGTH_SHORT).show()
                                      }
                                  }
                              }.onError { e ->
                                  AppLog.put("加载默认书籍失败", e)
                                  withContext(kotlinx.coroutines.Dispatchers.Main) {
                                      binding.cardBooksSection.isVisible = false
                                      // 显示错误提示
                                      android.widget.Toast.makeText(requireContext(), "加载书籍失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                  }
                              }
                      } else {
                          AppLog.put("默认分类URL为空")
                          withContext(kotlinx.coroutines.Dispatchers.Main) {
                              binding.cardBooksSection.isVisible = false
                          }
                      }
                  } else {
                      AppLog.put("没有可用的分类")
                      withContext(kotlinx.coroutines.Dispatchers.Main) {
                          binding.cardBooksSection.isVisible = false
                          // 显示提示信息
                          android.widget.Toast.makeText(requireContext(), "该书源暂没有可用分类", android.widget.Toast.LENGTH_SHORT).show()
                      }
                  }
             } catch (e: Exception) {
                 AppLog.put("加载默认书籍异常", e)
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     binding.cardBooksSection.isVisible = false
                 }
             } finally {
                 isBookLoading = false
                 AppLog.put("默认书籍加载完成")
             }
         }
     }
     
     // 新增：增强版书籍加载方法（用于书源类型切换）
     private fun loadAndShowBooksEnhanced() {
         // 防止重复调用
         if (isBookLoading) {
             AppLog.put("书籍正在加载中，跳过重复调用: loadAndShowBooksEnhanced")
             return
         }
         
         bookLoadingJob?.cancel()
         bookLoadingJob = lifecycleScope.launch {
             try {
                 isBookLoading = true
                 AppLog.put("开始增强版书籍加载")
                 
                 val currentSource = viewModel.currentBookSource.value
                 if (currentSource == null) {
                     AppLog.put("当前书源为空，无法加载书籍")
                     return@launch
                 }
                 
                 // 显示加载状态
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     binding.cardBooksSection.isVisible = true
                     binding.tvBooksSectionTitle.text = "正在加载书籍..."
                     bookGridAdapter.setItems(emptyList())
                 }
                 
                 // 在IO线程获取分类列表
                 val categories = withContext(IO) {
                     try {
                         AppLog.put("开始获取分类列表")
                         val result = currentSource.exploreKinds()
                         AppLog.put("成功获取分类列表，数量: ${result.size}")
                         result.forEach { category ->
                             AppLog.put("分类: ${category.title} - URL: ${category.url}")
                         }
                         result
                     } catch (e: Exception) {
                         AppLog.put("获取分类列表失败", e)
                         emptyList()
                     }
                 }
                 
                 if (categories.isNotEmpty()) {
                     // 选择第一个分类作为默认显示
                     val firstCategory = categories.first()
                     if (!firstCategory.url.isNullOrBlank()) {
                         AppLog.put("使用默认分类: ${firstCategory.title}, URL: ${firstCategory.url}")
                         
                         // 尝试多种方式获取书籍
                         var booksLoaded = false
                         
                         // 方法1：使用探索API
                         try {
                             AppLog.put("尝试方法1：使用探索API")
                             WebBook.exploreBook(lifecycleScope, currentSource, firstCategory.url)
                                 .timeout(30000L)
                                 .onSuccess(IO) { searchBooks ->
                                     AppLog.put("探索API成功，获取书籍数量: ${searchBooks.size}")
                                     
                                     if (searchBooks.isNotEmpty()) {
                                         booksLoaded = true
                                         val validBooks = searchBooks.filter { book ->
                                             !book.name.isBlank() && 
                                             !book.author.isBlank() && 
                                             !book.bookUrl.isBlank()
                                         }
                                         
                                         AppLog.put("探索API数据完整性检查：总书籍 ${searchBooks.size}，有效书籍 ${validBooks.size}")
                                         
                                         if (validBooks.isNotEmpty()) {
                                                                                           withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                  binding.cardBooksSection.isVisible = true
                                                  binding.tvBooksSectionTitle.text = "${firstCategory.title} - 热门书籍 (${validBooks.size}本)"
                                                  // 显示全部书籍，不再限制数量
                                                  bookGridAdapter.setItems(validBooks)
                                                  AppLog.put("探索API书籍加载成功")
                                              }
                                             // 标记已找到书籍，退出方法
                                             return@onSuccess
                                         }
                                     }
                                 }.onError { e ->
                                     AppLog.put("探索API失败", e)
                                 }
                         } catch (e: Exception) {
                             AppLog.put("探索API异常", e)
                         }
                         
                         // 方法2：如果探索API失败，尝试搜索热门关键词
                         if (!booksLoaded) {
                             try {
                                 AppLog.put("尝试方法2：搜索热门关键词")
                                 val searchKeywords = listOf("热门", "推荐", "最新", "完结")
                                 
                                 for (keyword in searchKeywords) {
                                     try {
                                         WebBook.searchBook(lifecycleScope, currentSource, keyword)
                                             .timeout(15000L)
                                             .onSuccess(IO) { searchBooks ->
                                                 AppLog.put("搜索关键词 '$keyword' 成功，获取书籍数量: ${searchBooks.size}")
                                                 
                                                 if (searchBooks.isNotEmpty()) {
                                                     val validBooks = searchBooks.filter { book ->
                                                         !book.name.isBlank() && 
                                                         !book.author.isBlank() && 
                                                         !book.bookUrl.isBlank()
                                                     }
                                                     
                                                                                              if (validBooks.isNotEmpty()) {
                                             booksLoaded = true
                                                                                           withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                  binding.cardBooksSection.isVisible = true
                                                  binding.tvBooksSectionTitle.text = "热门推荐 (${validBooks.size}本)"
                                                  // 显示全部书籍，不再限制数量
                                                  bookGridAdapter.setItems(validBooks)
                                                  AppLog.put("搜索关键词 '$keyword' 书籍加载成功")
                                              }
                                             // 标记已找到书籍，退出循环
                                             return@onSuccess
                                         }
                                                 }
                                             }.onError { e ->
                                                 AppLog.put("搜索关键词 '$keyword' 失败", e)
                                             }
                                     } catch (e: Exception) {
                                         AppLog.put("搜索关键词 '$keyword' 异常", e)
                                     }
                                 }
                             } catch (e: Exception) {
                                 AppLog.put("搜索关键词方法异常", e)
                             }
                         }
                         
                         // 如果所有方法都失败，显示错误状态
                         if (!booksLoaded) {
                             AppLog.put("所有书籍加载方法都失败")
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 binding.cardBooksSection.isVisible = true
                                 binding.tvBooksSectionTitle.text = "加载失败，请重试"
                                 bookGridAdapter.setItems(emptyList())
                                 android.widget.Toast.makeText(requireContext(), "书籍加载失败，请检查网络或稍后重试", android.widget.Toast.LENGTH_LONG).show()
                             }
                         }
                     } else {
                         AppLog.put("默认分类URL为空")
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             binding.cardBooksSection.isVisible = false
                         }
                     }
                 } else {
                     AppLog.put("没有可用的分类")
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         binding.cardBooksSection.isVisible = false
                         android.widget.Toast.makeText(requireContext(), "该书源暂没有可用分类", android.widget.Toast.LENGTH_SHORT).show()
                     }
                 }
             } catch (e: Exception) {
                 AppLog.put("增强版书籍加载异常", e)
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     binding.cardBooksSection.isVisible = false
                     android.widget.Toast.makeText(requireContext(), "书籍加载异常: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                 }
             } finally {
                 isBookLoading = false
                 AppLog.put("增强版书籍加载完成")
             }
         }
     }
     
     // 新增：根据分类加载书籍（增强版）
     private fun loadBooksByCategory(categoryName: String) {
         // 防抖检查
         val currentTime = System.currentTimeMillis()
         if (currentTime - lastCategoryClickTime < categoryClickDebounceTime) {
             AppLog.put("分类点击过于频繁，跳过: $categoryName")
             return
         }
         lastCategoryClickTime = currentTime
         
         // 防止重复调用
         if (isBookLoading) {
             AppLog.put("书籍正在加载中，跳过重复调用: $categoryName")
             return
         }
         
         bookLoadingJob?.cancel()
         bookLoadingJob = lifecycleScope.launch {
             try {
                 isBookLoading = true
                 AppLog.put("开始加载分类书籍: $categoryName")
                 
                 val currentSource = viewModel.currentBookSource.value
                 if (currentSource == null) {
                     AppLog.put("当前书源为空，无法加载书籍")
                     return@launch
                 }
                 
                 // 显示加载状态
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     binding.cardBooksSection.isVisible = true
                     binding.tvBooksSectionTitle.text = "加载中..."
                     bookGridAdapter.setItems(emptyList())
                 }
                 
                 // 在IO线程获取分类列表
                 val categories = withContext(IO) {
                     currentSource.exploreKinds()
                 }
                 
                 // 查找对应的分类
                 val targetCategory = categories.find { it.title == categoryName }
                 
                 if (targetCategory != null && !targetCategory.url.isNullOrBlank()) {
                     AppLog.put("找到分类: ${targetCategory.title}, URL: ${targetCategory.url}")
                     
                     // 调用探索API获取该分类下的真实书籍
                     WebBook.exploreBook(lifecycleScope, currentSource, targetCategory.url!!)
                         .timeout(30000L)
                         .onSuccess(IO) { searchBooks ->
                             AppLog.put("成功获取书籍，数量: ${searchBooks.size}")
                             
                             // 验证书籍数据的完整性
                             val validBooks = searchBooks.filter { book ->
                                 !book.name.isBlank() && 
                                 !book.author.isBlank() && 
                                 !book.bookUrl.isBlank()
                             }
                             
                             AppLog.put("分类书籍数据完整性检查：总书籍 ${searchBooks.size}，有效书籍 ${validBooks.size}")
                             
                             // 在主线程更新UI
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 if (validBooks.isNotEmpty()) {
                                                                           binding.cardBooksSection.isVisible = true
                                      binding.tvBooksSectionTitle.text = "${categoryName} - 热门书籍 (${validBooks.size}本)"
                                      // 显示全部书籍，不再限制数量
                                      bookGridAdapter.setItems(validBooks)
                                      AppLog.put("书籍列表更新完成，显示全部 ${validBooks.size} 本")
                                     
                                     // 记录第一本书的详细信息用于调试
                                     if (validBooks.isNotEmpty()) {
                                         val firstBook = validBooks.first()
                                         AppLog.put("分类第一本书详情: name=${firstBook.name}, author=${firstBook.author}, bookUrl=${firstBook.bookUrl}, intro=${firstBook.intro?.take(50)}")
                                         
                                         // 额外验证：检查书籍是否真的可以打开
                                         if (firstBook.bookUrl.isNotBlank()) {
                                             AppLog.put("✅ 第一本书可以正常打开: ${firstBook.name}")
                                         } else {
                                             AppLog.put("❌ 第一本书无法打开: ${firstBook.name}")
                                         }
                                     }
                                 } else {
                                     binding.cardBooksSection.isVisible = false
                                     AppLog.put("该分类下没有有效书籍")
                                     android.widget.Toast.makeText(requireContext(), "该分类下暂无有效书籍", android.widget.Toast.LENGTH_SHORT).show()
                                 }
                             }
                         }.onError { e ->
                             AppLog.put("加载分类书籍失败: $categoryName", e)
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 binding.cardBooksSection.isVisible = false
                             }
                         }
                 } else {
                     AppLog.put("未找到分类或分类URL为空: $categoryName")
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         binding.cardBooksSection.isVisible = false
                     }
                 }
             } catch (e: Exception) {
                 AppLog.put("加载分类书籍异常: $categoryName", e)
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     binding.cardBooksSection.isVisible = false
                 }
             } finally {
                 isBookLoading = false
                 AppLog.put("分类书籍加载完成: $categoryName")
             }
         }
     }
    
    // 新增：显示书籍长按菜单
    private fun showBookLongPressMenu(book: SearchBook) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), binding.root)
        popup.menu.add(0, 1, 0, "加入书架")
        popup.menu.add(0, 2, 1, "查看详情")
        popup.menu.add(0, 3, 2, "分享")
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    // 加入书架
                    viewModel.addToBookshelf(book)
                    true
                }
                                 2 -> {
                     // 查看详情 - 使用统一的打开方法
                     openBookDetail(book)
                     true
                 }
                3 -> {
                    // 分享
                    shareBook(book)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
    
         // 新增：更新分类芯片选中状态
     private fun updateCategoryChipSelection(selectedCategory: String) {
         try {
             // 先取消所有芯片的选中状态
             for (i in 0 until binding.chipGroupCategories.childCount) {
                 val chip = binding.chipGroupCategories.getChildAt(i) as? Chip
                 chip?.isChecked = false
             }
             
             // 找到对应的分类芯片并选中
             for (i in 0 until binding.chipGroupCategories.childCount) {
                 val chip = binding.chipGroupCategories.getChildAt(i) as? Chip
                 if (chip?.tag == selectedCategory) {
                     chip.isChecked = true
                     AppLog.put("更新分类芯片选中状态: $selectedCategory")
                     break
                 }
             }
         } catch (e: Exception) {
             AppLog.put("更新分类芯片选中状态失败", e)
         }
     }
     
     // 新增：分享书籍
     private fun shareBook(book: SearchBook) {
         val shareText = "${book.name} - ${book.author}\n${book.intro ?: ""}"
         val intent = android.content.Intent().apply {
             action = android.content.Intent.ACTION_SEND
             type = "text/plain"
             putExtra(android.content.Intent.EXTRA_TEXT, shareText)
         }
         startActivity(android.content.Intent.createChooser(intent, "分享书籍"))
     }
     
     // 新增：随机选择一个分类并直接跳转到完整书籍列表（优化版）
     private fun openRandomCategoryFullList() {
         try {
             AppLog.put("换一换：随机选择分类并打开完整书籍列表")
             
             val currentSource = viewModel.currentBookSource.value
             if (currentSource == null) {
                 AppLog.put("当前书源为空，无法打开随机分类列表")
                 android.widget.Toast.makeText(requireContext(), "请先选择书源", android.widget.Toast.LENGTH_SHORT).show()
                 return
             }
             
             // 显示加载提示
             android.widget.Toast.makeText(requireContext(), "正在随机选择分类...", android.widget.Toast.LENGTH_SHORT).show()
             
             // 异步获取分类信息并随机选择一个（增加超时控制）
             lifecycleScope.launch {
                 try {
                     // 设置超时，避免长时间等待
                     val categories = withContext(IO) {
                         try {
                             withTimeout(10000L) { // 10秒超时
                                 currentSource.exploreKinds()
                             }
                         } catch (e: Exception) {
                             AppLog.put("获取分类超时或失败", e)
                             emptyList()
                         }
                     }
                     
                     if (categories.isEmpty()) {
                         AppLog.put("书源没有可用分类")
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             android.widget.Toast.makeText(requireContext(), "该书源暂没有可用分类", android.widget.Toast.LENGTH_SHORT).show()
                         }
                         return@launch
                     }
                     
                     // 随机选择一个分类
                     val randomCategory = categories.random()
                     AppLog.put("随机选择分类: ${randomCategory.title}")
                     
                     // 设置分类状态
                     viewModel.setCategory(randomCategory.title)
                     
                     // 更新分类芯片的选中状态
                     updateCategoryChipSelection(randomCategory.title)
                     
                     if (!randomCategory.url.isNullOrBlank()) {
                         AppLog.put("随机分类URL: ${randomCategory.url}")
                         
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             // 直接跳转到完整书籍列表
                             startActivity<ExploreShowActivity> {
                                 putExtra("sourceUrl", currentSource.bookSourceUrl)
                                 putExtra("exploreUrl", randomCategory.url)
                                 putExtra("exploreName", "${randomCategory.title} - 全部书籍")
                             }
                             AppLog.put("成功启动随机分类完整书籍列表: ${randomCategory.title}")
                         }
                     } else {
                         // 如果随机分类URL为空，尝试使用默认探索
                         if (!currentSource.exploreUrl.isNullOrBlank()) {
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 startActivity<ExploreShowActivity> {
                                     putExtra("sourceUrl", currentSource.bookSourceUrl)
                                     putExtra("exploreUrl", currentSource.exploreUrl)
                                     putExtra("exploreName", "${randomCategory.title} - 全部书籍")
                                 }
                                 AppLog.put("使用默认探索URL启动随机分类列表: ${randomCategory.title}")
                             }
                         } else {
                             AppLog.put("书源没有探索URL，无法查看随机分类书籍")
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 android.widget.Toast.makeText(requireContext(), "该书源暂不支持探索功能", android.widget.Toast.LENGTH_SHORT).show()
                             }
                         }
                     }
                 } catch (e: Exception) {
                     AppLog.put("获取随机分类信息失败", e)
                     // 尝试使用默认探索
                     if (!currentSource.exploreUrl.isNullOrBlank()) {
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             startActivity<ExploreShowActivity> {
                                 putExtra("sourceUrl", currentSource.bookSourceUrl)
                                 putExtra("exploreUrl", currentSource.exploreUrl)
                                 putExtra("exploreName", "随机分类 - 全部书籍")
                             }
                             AppLog.put("异常情况下使用默认探索URL")
                         }
                     } else {
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             android.widget.Toast.makeText(requireContext(), "获取随机分类信息失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                         }
                     }
                 }
             }
         } catch (e: Exception) {
             AppLog.put("打开随机分类完整书籍列表失败", e)
             android.widget.Toast.makeText(requireContext(), "打开书籍列表失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
         }
     }
     
     // 新增：智能书籍加载策略 - 预加载完整数据，显示有限书籍
     private fun loadBooksWithPreload(categoryName: String, showLimited: Boolean = true) {
         try {
             AppLog.put("智能书籍加载: $categoryName, 显示限制: $showLimited")
             
             val currentSource = viewModel.currentBookSource.value
             if (currentSource == null) {
                 AppLog.put("当前书源为空，无法加载书籍")
                 android.widget.Toast.makeText(requireContext(), "请先选择书源", android.widget.Toast.LENGTH_SHORT).show()
                 return
             }
             
             // 动漫类书源特殊处理日志
             if (currentSource.bookSourceType == BookSourceType.image) {
                 AppLog.put("🎯 动漫类书源特殊处理: ${currentSource.bookSourceName}")
                 AppLog.put("🎯 分类信息: $categoryName")
             }
             
             // 如果显示限制书籍，先跳转到完整列表预加载数据
             if (!showLimited) {
                 openCategoryFullList(categoryName)
                 return
             }
             
             // 显示加载状态
             binding.cardBooksSection.isVisible = true
             binding.tvBooksSectionTitle.text = "加载中..."
             bookGridAdapter.setItems(emptyList())
             
             // 异步获取分类信息并加载书籍
             lifecycleScope.launch {
                 try {
                     // 设置超时，避免长时间等待
                     val categories = withContext(IO) {
                         try {
                             withTimeout(10000L) { // 10秒超时
                                 currentSource.exploreKinds()
                             }
                         } catch (e: Exception) {
                             AppLog.put("获取分类超时或失败: $categoryName", e)
                             emptyList()
                         }
                     }
                     
                     // 动漫类书源分类信息详细日志
                     if (currentSource.bookSourceType == BookSourceType.image) {
                         AppLog.put("🎯 动漫类书源分类数量: ${categories.size}")
                         categories.forEach { category ->
                             AppLog.put("🎯 分类: ${category.title} - URL: ${category.url}")
                         }
                     }
                     
                     if (categories.isEmpty()) {
                         AppLog.put("书源没有可用分类: $categoryName")
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             binding.cardBooksSection.isVisible = false
                             android.widget.Toast.makeText(requireContext(), "该书源暂没有可用分类", android.widget.Toast.LENGTH_SHORT).show()
                         }
                         return@launch
                     }
                     
                     val targetCategory = categories.find { it.title == categoryName }
                     if (targetCategory != null && !targetCategory.url.isNullOrBlank()) {
                         AppLog.put("找到分类: ${targetCategory.title}, URL: ${targetCategory.url}")
                         
                         // 动漫类书源：尝试多种加载策略
                         if (currentSource.bookSourceType == BookSourceType.image) {
                             loadAnimeBooksWithFallback(currentSource, targetCategory, categoryName)
                         } else {
                             // 其他类型书源：使用原有逻辑
                             loadNormalBooks(currentSource, targetCategory, categoryName)
                         }
                     } else {
                         AppLog.put("未找到分类或分类URL为空: $categoryName")
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             binding.cardBooksSection.isVisible = false
                             android.widget.Toast.makeText(requireContext(), "该分类暂不支持探索功能", android.widget.Toast.LENGTH_SHORT).show()
                         }
                     }
                 } catch (e: Exception) {
                     AppLog.put("获取分类信息失败", e)
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         binding.cardBooksSection.isVisible = false
                         android.widget.Toast.makeText(requireContext(), "获取分类信息失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                     }
                 }
             }
         } catch (e: Exception) {
             AppLog.put("智能书籍加载失败: $categoryName", e)
             android.widget.Toast.makeText(requireContext(), "加载书籍失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
         }
     }
               
    // 重新设计：简化的动漫类书源加载逻辑
    private suspend fun loadAnimeBooksWithFallback(
        currentSource: BookSource,
        targetCategory: ExploreKind,
        categoryName: String
    ) {
        try {
            val startTime = System.currentTimeMillis()
            AppLog.put("开始动漫类书源加载: ${categoryName}")
            
            // 立即显示加载状态
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                binding.cardBooksSection.isVisible = true
                binding.tvBooksSectionTitle.text = "正在加载 ${categoryName}..."
                bookGridAdapter.setItems(emptyList())
            }
            
            // 只使用探索API，简化逻辑
            WebBook.exploreBook(lifecycleScope, currentSource, targetCategory.url!!)
                .timeout(15000L) // 减少超时时间到15秒
                .onSuccess(IO) { searchBooks ->
                    AppLog.put("探索API成功，获取书籍数量: ${searchBooks.size}")
                    
                    // 直接使用原始数据，不进行复杂验证
                    if (searchBooks.isNotEmpty()) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            // 显示所有书籍，最多12本（减少数据处理）
                            val displayBooks = if (searchBooks.size > 12) searchBooks.take(12) else searchBooks
                            bookGridAdapter.setItems(displayBooks)
                            binding.tvBooksSectionTitle.text = "${categoryName} (${searchBooks.size}本)"
                            val loadTime = System.currentTimeMillis() - startTime
                            AppLog.put("动漫类加载成功，显示 ${displayBooks.size} 本，耗时 ${loadTime}ms")
                        }
                    } else {
                        // 数据为空时的简单处理
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            binding.tvBooksSectionTitle.text = "${categoryName} (0本)"
                            bookGridAdapter.setItems(emptyList())
                            AppLog.put("动漫类数据为空")
                        }
                    }
                }
                .onError { e ->
                    val loadTime = System.currentTimeMillis() - startTime
                    AppLog.put("探索API失败，耗时 ${loadTime}ms", e)
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.tvBooksSectionTitle.text = "加载失败，请重试"
                        bookGridAdapter.setItems(emptyList())
                    }
                }
                
        } catch (e: Exception) {
            AppLog.put("动漫类书源加载异常", e)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                binding.cardBooksSection.isVisible = true
                binding.tvBooksSectionTitle.text = "加载异常，请重试"
                bookGridAdapter.setItems(emptyList())
            }
        }
    }
     
     // 新增：普通书源加载（保持原有逻辑）
     private suspend fun loadNormalBooks(
         currentSource: BookSource,
         targetCategory: ExploreKind,
         categoryName: String
     ) {
         try {
             AppLog.put("普通书源加载: ${targetCategory.title}")
             
             // 调用探索API获取该分类下的书籍
             WebBook.exploreBook(lifecycleScope, currentSource, targetCategory.url!!)
                 .timeout(30000L)
                 .onSuccess(IO) { searchBooks ->
                     AppLog.put("成功获取分类书籍，数量: ${searchBooks.size}")
                     
                     // 验证书籍数据的完整性
                     val validBooks = searchBooks.filter { book ->
                         !book.name.isBlank() && 
                         !book.author.isBlank() && 
                         !book.bookUrl.isBlank()
                     }
                     
                     AppLog.put("分类书籍数据完整性检查：总书籍 ${searchBooks.size}，有效书籍 ${validBooks.size}")
                     
                     // 在主线程更新UI，显示限制数量的书籍
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         if (validBooks.isNotEmpty()) {
                             binding.cardBooksSection.isVisible = true
                             binding.tvBooksSectionTitle.text = "${categoryName} - 热门书籍 (${validBooks.size}本)"
                             
                             // 显示9-12本书，支持滚动查看更多
                             val displayBooks = if (validBooks.size > 12) validBooks.take(12) else validBooks
                             bookGridAdapter.setItems(displayBooks)
                             AppLog.put("分类书籍加载完成，显示 ${displayBooks.size} 本，总共 ${validBooks.size} 本")
                             
                             // 记录第一本书的详细信息用于调试
                             if (displayBooks.isNotEmpty()) {
                                 val firstBook = displayBooks.first()
                                 AppLog.put("分类第一本书详情: name=${firstBook.name}, author=${firstBook.author}, bookUrl=${firstBook.bookUrl}, intro=${firstBook.intro?.take(50)}")
                             }
                         } else {
                             binding.cardBooksSection.isVisible = false
                             AppLog.put("该分类下没有有效书籍")
                             android.widget.Toast.makeText(requireContext(), "该分类下暂无有效书籍", android.widget.Toast.LENGTH_SHORT).show()
                         }
                     }
                 }.onError { e ->
                     AppLog.put("加载分类书籍失败: $categoryName", e)
                     withContext(kotlinx.coroutines.Dispatchers.Main) {
                         binding.cardBooksSection.isVisible = false
                         android.widget.Toast.makeText(requireContext(), "加载书籍失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                     }
                 }
         } catch (e: Exception) {
             AppLog.put("普通书源加载异常: $categoryName", e)
             withContext(kotlinx.coroutines.Dispatchers.Main) {
                 binding.cardBooksSection.isVisible = false
                 android.widget.Toast.makeText(requireContext(), "加载书籍异常: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
             }
         }
     }
     
     // 新增：直接打开分类的完整书籍列表（优化版）
     private fun openCategoryFullList(categoryName: String) {
         try {
             AppLog.put("直接打开分类完整书籍列表: $categoryName")
             
             val currentSource = viewModel.currentBookSource.value
             if (currentSource == null) {
                 AppLog.put("当前书源为空，无法打开分类列表")
                 android.widget.Toast.makeText(requireContext(), "请先选择书源", android.widget.Toast.LENGTH_SHORT).show()
                 return
             }
             
                           // 动漫类书源特殊处理日志
              if (currentSource.bookSourceType == BookSourceType.image) {
                  AppLog.put("🎯 动漫类书源打开完整列表: $categoryName")
              }
             
             // 显示加载提示
             android.widget.Toast.makeText(requireContext(), "正在加载分类: $categoryName", android.widget.Toast.LENGTH_SHORT).show()
             
             // 异步获取分类信息并跳转（增加超时控制）
             lifecycleScope.launch {
                 try {
                     // 设置超时，避免长时间等待
                     val categories = withContext(IO) {
                         try {
                             withTimeout(10000L) { // 10秒超时
                                 currentSource.exploreKinds()
                             }
                         } catch (e: Exception) {
                             AppLog.put("获取分类超时或失败: $categoryName", e)
                             emptyList()
                         }
                     }
                     
                     if (categories.isEmpty()) {
                         AppLog.put("书源没有可用分类: $categoryName")
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             android.widget.Toast.makeText(requireContext(), "该书源暂没有可用分类", android.widget.Toast.LENGTH_SHORT).show()
                         }
                         return@launch
                     }
                     
                                           val targetCategory = categories.find { it.title == categoryName }
                      if (targetCategory != null && !targetCategory.url.isNullOrBlank()) {
                          AppLog.put("找到分类: ${targetCategory.title}, URL: ${targetCategory.url}")
                          
                          withContext(kotlinx.coroutines.Dispatchers.Main) {
                              // 直接跳转到完整书籍列表
                              startActivity<ExploreShowActivity> {
                                  putExtra("sourceUrl", currentSource.bookSourceUrl)
                                  putExtra("exploreUrl", targetCategory.url)
                                  putExtra("exploreName", "$categoryName - 全部书籍")
                              }
                              AppLog.put("成功启动分类完整书籍列表: $categoryName")
                          }
                     } else {
                         // 如果找不到分类URL，尝试使用默认探索
                         if (!currentSource.exploreUrl.isNullOrBlank()) {
                             AppLog.put("使用默认探索URL: ${currentSource.exploreUrl}")
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 startActivity<ExploreShowActivity> {
                                     putExtra("sourceUrl", currentSource.bookSourceUrl)
                                     putExtra("exploreUrl", currentSource.exploreUrl)
                                     putExtra("exploreName", "$categoryName - 全部书籍")
                                 }
                                 AppLog.put("使用默认探索URL启动分类列表: $categoryName")
                             }
                         } else {
                             AppLog.put("书源没有探索URL，无法查看分类书籍: $categoryName")
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 android.widget.Toast.makeText(requireContext(), "该书源暂不支持探索功能", android.widget.Toast.LENGTH_SHORT).show()
                             }
                         }
                     }
                 } catch (e: Exception) {
                     AppLog.put("获取分类信息失败: $categoryName", e)
                     // 尝试使用默认探索
                     if (!currentSource.exploreUrl.isNullOrBlank()) {
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             startActivity<ExploreShowActivity> {
                                 putExtra("sourceUrl", currentSource.bookSourceUrl)
                                 putExtra("exploreUrl", currentSource.exploreUrl)
                                 putExtra("exploreName", "$categoryName - 全部书籍")
                             }
                             AppLog.put("异常情况下使用默认探索URL: $categoryName")
                         }
                     } else {
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             android.widget.Toast.makeText(requireContext(), "获取分类信息失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                         }
                     }
                 }
             }
         } catch (e: Exception) {
             AppLog.put("打开分类完整书籍列表失败: $categoryName", e)
             android.widget.Toast.makeText(requireContext(), "打开书籍列表失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
         }
     }
     
     // 新增：统一处理书籍详情页打开（增强版）
     private fun openBookDetail(book: SearchBook) {
         try {
             AppLog.put("打开书籍详情页: ${book.name} - ${book.author}")
             AppLog.put("书籍完整数据: name=${book.name}, author=${book.author}, bookUrl=${book.bookUrl}")
             AppLog.put("书籍额外数据: intro=${book.intro?.take(50)}, coverUrl=${book.coverUrl}, wordCount=${book.wordCount}")
             
             // 获取当前书源信息
             val currentSource = viewModel.currentBookSource.value
             val isAnimeSource = currentSource?.bookSourceType == BookSourceType.image
             
             // 构建完整的书籍信息
             val bookInfo = Bundle().apply {
                 putString("name", book.name)
                 putString("author", book.author ?: "未知作者")
                 putString("bookUrl", book.bookUrl)
                 putString("intro", book.intro ?: "暂无简介")
                 putString("coverUrl", book.coverUrl ?: "")
                 putString("wordCount", book.wordCount ?: "0")
                 putString("latestChapterTitle", book.latestChapterTitle ?: "暂无章节信息")
                 
                 // 书源相关信息
                 putString("sourceUrl", currentSource?.bookSourceUrl ?: "")
                 putString("sourceName", currentSource?.bookSourceName ?: "")
                 putInt("sourceType", currentSource?.bookSourceType ?: 0)
                 
                 // 分类信息
                 putString("category", viewModel.selectedCategory.value ?: "")
                 
                 // 动漫类书源特殊处理
                 if (isAnimeSource) {
                     putBoolean("isAnime", true)
                     putString("animeStatus", "连载中") // 默认状态
                 }
             }
             
             AppLog.put("准备启动 BookInfoActivity，传递参数数量: ${bookInfo.size()}")
             
             // 跳转到书籍详情
             startActivity<BookInfoActivity> {
                 putExtras(bookInfo)
             }
             
             AppLog.put("成功启动 BookInfoActivity")
             
         } catch (e: Exception) {
             AppLog.put("启动 BookInfoActivity 失败", e)
             
             // 尝试使用备用方法 - 直接传递核心参数
             try {
                 val intent = android.content.Intent(requireContext(), BookInfoActivity::class.java).apply {
                     putExtra("name", book.name)
                     putExtra("author", book.author ?: "未知作者")
                     putExtra("bookUrl", book.bookUrl)
                     putExtra("intro", book.intro ?: "暂无简介")
                 }
                 startActivity(intent)
                 AppLog.put("使用备用方法启动 BookInfoActivity 成功")
             } catch (e2: Exception) {
                 AppLog.put("备用方法也失败", e2)
                 
                 // 最后尝试：只传递最基本的参数
                 try {
                     val simpleIntent = android.content.Intent(requireContext(), BookInfoActivity::class.java).apply {
                         putExtra("name", book.name)
                         putExtra("bookUrl", book.bookUrl)
                     }
                     startActivity(simpleIntent)
                     AppLog.put("使用最简单方法启动 BookInfoActivity 成功")
                 } catch (e3: Exception) {
                     AppLog.put("所有启动方法都失败", e3)
                     android.widget.Toast.makeText(requireContext(), "打开书籍详情失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
                 }
             }
         }
     }
 }
