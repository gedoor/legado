package io.legado.app.ui.rss.article


import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.databinding.FragmentRssArticlesBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RssArticlesFragment() : VMBaseFragment<RssArticlesViewModel>(R.layout.fragment_rss_articles),
    BaseRssArticlesAdapter.CallBack {

    constructor(sortName: String, sortUrl: String) : this() {
        arguments = Bundle().apply {
            putString("sortName", sortName)
            putString("sortUrl", sortUrl)
        }
    }

    private val binding by viewBinding(FragmentRssArticlesBinding::bind)
    private val activityViewModel by activityViewModels<RssSortViewModel>()
    override val viewModel by viewModels<RssArticlesViewModel>()
    private val adapter: BaseRssArticlesAdapter<*> by lazy {
        when (activityViewModel.rssSource?.articleStyle) {
            1 -> RssArticlesAdapter1(requireContext(), this@RssArticlesFragment)
            2 -> RssArticlesAdapter2(requireContext(), this@RssArticlesFragment)
            else -> RssArticlesAdapter(requireContext(), this@RssArticlesFragment)
        }
    }
    private val loadMoreView: LoadMoreView by lazy {
        LoadMoreView(requireContext())
    }
    private var articlesFlowJob: Job? = null
    override val isGridLayout: Boolean
        get() = activityViewModel.isGridLayout

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.init(arguments)
        initView()
        initData()
    }

    private fun initView() = binding.run {
        refreshLayout.setColorSchemeColors(accentColor)
        recyclerView.setEdgeEffectColor(primaryColor)
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                scrollToBottom(true)
            }
        }
        recyclerView.layoutManager = if (activityViewModel.isGridLayout) {
            recyclerView.setPadding(8, 0, 8, 0)
            GridLayoutManager(requireContext(), 2)
        } else {
            recyclerView.addItemDecoration(VerticalDivider(requireContext()))
            LinearLayoutManager(requireContext())
        }
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
                }
            }
        })
        refreshLayout.post {
            refreshLayout.isRefreshing = true
            loadArticles()
        }
    }

    private fun initData() {
        val rssUrl = activityViewModel.url ?: return
        articlesFlowJob?.cancel()
        articlesFlowJob = lifecycleScope.launch {
            appDb.rssArticleDao.flowByOriginSort(rssUrl, viewModel.sortName).catch {
                AppLog.put("订阅文章界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    private fun loadArticles() {
        activityViewModel.rssSource?.let {
            viewModel.loadArticles(it)
        }
    }

    private fun scrollToBottom(forceLoad: Boolean = false) {
        if (viewModel.isLoading) return
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
        activityViewModel.read(rssArticle)
        startActivity<ReadRssActivity> {
            putExtra("title", rssArticle.title)
            putExtra("origin", rssArticle.origin)
            putExtra("link", rssArticle.link)
        }
    }
}