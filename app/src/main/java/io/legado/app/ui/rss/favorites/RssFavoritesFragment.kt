package io.legado.app.ui.rss.favorites


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssStar
import io.legado.app.databinding.FragmentRssArticlesBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RssFavoritesFragment() : VMBaseFragment<RssFavoritesViewModel>(R.layout.fragment_rss_articles),
    RssFavoritesAdapter.CallBack {

    constructor(group: String) : this() {
        arguments = Bundle().apply {
            putString("group", group)
        }
    }

    private val binding by viewBinding(FragmentRssArticlesBinding::bind)
    override val viewModel by viewModels<RssFavoritesViewModel>()
    private val adapter: RssFavoritesAdapter by lazy {
        RssFavoritesAdapter(requireContext(), this@RssFavoritesFragment)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        loadArticles()
    }

    private fun initView() = binding.run {
        refreshLayout.isEnabled = false
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.layoutManager = run {
            recyclerView.addItemDecoration(VerticalDivider(requireContext()))
            LinearLayoutManager(requireContext())
        }
        recyclerView.adapter = adapter
    }

    private fun loadArticles() {
        lifecycleScope.launch {
            val group = arguments?.getString("group") ?: "默认分组"
            appDb.rssStarDao.flowByGroup(group).catch {
                AppLog.put("订阅文章界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }
}
