package io.legado.app.ui.rss.article

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.databinding.FragmentRssArticlesBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.rss.read.ReadRss
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyNavigationBarPadding
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RssArticlesFragment() : VMBaseFragment<RssArticlesViewModel>(R.layout.fragment_rss_articles),
    BaseRssArticlesAdapter.CallBack {

    constructor(sortName: String, sortUrl: String, searchKey: String?) : this() {
        arguments = Bundle().apply {
            putString("sortName", sortName)
            putString("sortUrl", sortUrl)
            putString("searchKey", searchKey)
        }
    }

    private val binding by viewBinding(FragmentRssArticlesBinding::bind)
    private val activityViewModel by activityViewModels<RssSortViewModel>()
    override val viewModel by viewModels<RssArticlesViewModel>()
    private val isPreload by lazy { activityViewModel.rssSource?.preload ?: false }
    private val adapter: BaseRssArticlesAdapter<*> by lazy {
        when (activityViewModel.rssSource?.articleStyle) {
            1 -> RssArticlesAdapter1(requireContext(), this@RssArticlesFragment)
            2 -> RssArticlesAdapter2(requireContext(), this@RssArticlesFragment)
            3 -> RssArticlesAdapter3(requireContext(), this@RssArticlesFragment)
            else -> RssArticlesAdapter(requireContext(), this@RssArticlesFragment)
        }
    }
    private val loadMoreView: LoadMoreView by lazy {
        LoadMoreView(requireContext())
    }
    private var articlesFlowJob: Job? = null
    override val isGridLayout: Boolean
        get() = activityViewModel.isGridLayout
    private var fullRefresh = true

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.init(arguments)
        initView()
        initData()
    }

    private fun initView() = binding.run {
        refreshLayout.setColorSchemeColors(accentColor)
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.applyNavigationBarPadding()
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                scrollToBottom(true)
            }
        }
        val layoutManager = if (activityViewModel.isWaterLayout) {
            recyclerView.itemAnimator = null
            recyclerView.setPadding(4, 0, 4, 0)
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        } else if (activityViewModel.isGridLayout) {
            recyclerView.setPadding(8, 0, 8, 0)
            GridLayoutManager(requireContext(), 2)
        } else {
            recyclerView.addItemDecoration(VerticalDivider(requireContext()))
            LinearLayoutManager(requireContext())
        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        adapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        refreshLayout.setOnRefreshListener {
            loadArticles()
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                    return
                }
                if (layoutManager is StaggeredGridLayoutManager) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPositions = layoutManager.findFirstVisibleItemPositions(null)
                    val firstVisibleItemPosition = firstVisibleItemPositions?.minOrNull() ?: 0
                    if (isPreload  && (visibleItemCount + firstVisibleItemPosition) >= (totalItemCount - 5)) {
                        scrollToBottom()
                    }
                }
            }
        })
        if (isPreload) {
            refreshLayout.post {
                refreshLayout.isRefreshing = true
                loadArticles()
            }
            return@run
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                refreshLayout.isRefreshing = true
                loadArticles()
                this@launch.cancel()
            }
        } //只刷新可见页面,非预加载时使用
    }

    @OptIn(FlowPreview::class)
    private fun initData() {
        val rssUrl = activityViewModel.url ?: return
        articlesFlowJob?.cancel()
        articlesFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.rssArticleDao.flowByOriginSort(rssUrl, viewModel.sortName)
                .debounce(200L) // 200毫秒防抖
                .catch {
                    AppLog.put("订阅文章界面获取数据失败\n${it.localizedMessage}", it)
                }.flowOn(IO).collect { newList ->
                    if (fullRefresh || newList.isEmpty()) {
                        adapter.setItems(newList)
                    } else {
                        //用DiffUtil只对差异数据进行更新
                        //注意RecyclerView的复用机制,切换标签时采用差异化更新会报ViewHolder的状态管理混乱
                        adapter.setItems(newList, object : DiffUtil.ItemCallback<RssArticle>() {
                            override fun areItemsTheSame(
                                oldItem: RssArticle, newItem: RssArticle
                            ): Boolean {
                                return oldItem.link == newItem.link
                            }

                            override fun areContentsTheSame(
                                oldItem: RssArticle, newItem: RssArticle
                            ): Boolean {
                                return oldItem.title == newItem.title && oldItem.image == newItem.image && oldItem.read == newItem.read
                            }

                            override fun getChangePayload(
                                oldItem: RssArticle, newItem: RssArticle
                            ): Any? {
                                return if (oldItem.read != newItem.read) { "read" }
                                else if (oldItem.title != newItem.title) { "title" }
                                else { null }
                            }
                        }, true)
                    }
                }
        }
    }

    private fun loadArticles() {
        fullRefresh = true
        activityViewModel.rssSource?.let {
            viewModel.loadArticles(it)
        }
    }

    private fun scrollToBottom(forceLoad: Boolean = false) {
        if (viewModel.isLoading) return
        fullRefresh = false
        if ((loadMoreView.hasMore && adapter.getActualItemCount() > 0) || forceLoad) {
            loadMoreView.hasMore()
            activityViewModel.rssSource?.let {
                viewModel.loadMore(it)
            }
        }
    }

    override fun observeLiveBus() {
        viewModel.loadErrorLiveData.observe(viewLifecycleOwner) {
            loadMoreView.error(it)
        }
        viewModel.loadFinallyLiveData.observe(viewLifecycleOwner) { hasMore ->
            binding.refreshLayout.isRefreshing = false
            if (!hasMore) {
                loadMoreView.noMore()
            }
        }
    }

    override fun readRss(rssArticle: RssArticle) {
        fullRefresh = false //activityViewModel.read会触发数据库更新,此时进行差异化更新
        activityViewModel.read(rssArticle)
        ReadRss.readRss(this, rssArticle, activityViewModel.rssSource)
    }
}