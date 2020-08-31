package io.legado.app.ui.rss.article

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.RssArticle
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.getViewModel
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.startActivity
import kotlinx.android.synthetic.main.fragment_rss_articles.*
import kotlinx.android.synthetic.main.view_load_more.view.*
import kotlinx.android.synthetic.main.view_refresh_recycler.*

class RssArticlesFragment : VMBaseFragment<RssArticlesViewModel>(R.layout.fragment_rss_articles),
    RssArticlesAdapter.CallBack {

    companion object {
        fun create(sortName: String, sortUrl: String): RssArticlesFragment {
            return RssArticlesFragment().apply {
                val bundle = Bundle()
                bundle.putString("sortName", sortName)
                bundle.putString("sortUrl", sortUrl)
                arguments = bundle
            }
        }
    }

    private val activityViewModel: RssSortViewModel
        get() = getViewModelOfActivity(RssSortViewModel::class.java)
    override val viewModel: RssArticlesViewModel
        get() = getViewModel(RssArticlesViewModel::class.java)
    lateinit var adapter: RssArticlesAdapter
    private lateinit var loadMoreView: LoadMoreView
    private var rssArticlesData: LiveData<List<RssArticle>>? = null
    override val isGridLayout: Boolean
        get() = activityViewModel.isGridLayout

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.init(arguments)
        initView()
        refresh_recycler_view.startLoading()
        initView()
        initData()
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = if (activityViewModel.isGridLayout) {
            recycler_view.setPadding(8, 0, 8, 0)
            GridLayoutManager(requireContext(), 2)
        } else {
            recycler_view.addItemDecoration(VerticalDivider(requireContext()))
            LinearLayoutManager(requireContext())

        }
        adapter = RssArticlesAdapter(requireContext(), activityViewModel.layoutId, this)
        recycler_view.adapter = adapter
        loadMoreView = LoadMoreView(requireContext())
        adapter.addFooterView(loadMoreView)
        refresh_recycler_view.onRefreshStart = {
            activityViewModel.rssSource?.let {
                viewModel.loadContent(it)
            }
        }
        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun initData() {
        activityViewModel.url?.let {
            rssArticlesData?.removeObservers(this)
            rssArticlesData = App.db.rssArticleDao().liveByOriginSort(it, viewModel.sortName)
            rssArticlesData?.observe(viewLifecycleOwner, { list ->
                adapter.setItems(list)
            })
        }
    }

    private fun scrollToBottom() {
        if (viewModel.isLoading) return
        if (loadMoreView.hasMore && adapter.getActualItemCount() > 0) {
            loadMoreView.rotate_loading.show()
            activityViewModel.rssSource?.let {
                viewModel.loadMore(it)
            }
        }
    }

    override fun observeLiveBus() {
        viewModel.loadFinally.observe(viewLifecycleOwner, {
            refresh_recycler_view.stopLoading()
            if (it) {
                loadMoreView.startLoad()
            } else {
                loadMoreView.noMore()
            }
        })
    }

    override fun readRss(rssArticle: RssArticle) {
        activityViewModel.read(rssArticle)
        startActivity<ReadRssActivity>(
            Pair("title", rssArticle.title),
            Pair("origin", rssArticle.origin),
            Pair("link", rssArticle.link)
        )
    }
}