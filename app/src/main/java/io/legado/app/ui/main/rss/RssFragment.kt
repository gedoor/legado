package io.legado.app.ui.main.rss

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.view.isGone
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.FragmentRssBinding
import io.legado.app.databinding.ItemRssBinding
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.ui.rss.favorites.RssFavoritesActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.rss.source.manage.RssSourceActivity
import io.legado.app.ui.rss.source.manage.RssSourceViewModel
import io.legado.app.ui.rss.subscription.SourceSubActivity

import io.legado.app.utils.getViewModel
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import org.jetbrains.anko.sdk27.listeners.onClick

/**
 * 订阅界面
 */
class RssFragment : VMBaseFragment<RssSourceViewModel>(R.layout.fragment_rss),
    RssAdapter.CallBack {
    private val binding by viewBinding(FragmentRssBinding::bind)
    private lateinit var adapter: RssAdapter
    override val viewModel: RssSourceViewModel
        get() = getViewModel(RssSourceViewModel::class.java)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initRecyclerView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_rss, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_rss_config -> startActivity<RssSourceActivity>()
            R.id.menu_rss_star -> startActivity<RssFavoritesActivity>()
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        adapter = RssAdapter(requireContext(), this)
        binding.recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemRssBinding.inflate(layoutInflater, it, false).apply {
                tvName.setText(R.string.source_subscription)
                ivIcon.setImageResource(R.mipmap.ic_launcher)
                root.onClick {
                    startActivity<SourceSubActivity>()
                }
            }
        }
    }

    private fun initData() {
        App.db.rssSourceDao().liveEnabled().observe(viewLifecycleOwner, {
            binding.tvEmptyMsg.isGone = it.isNotEmpty()
            adapter.setItems(it)
        })
    }

    override fun openRss(rssSource: RssSource) {
        startActivity<RssSortActivity>(Pair("url", rssSource.sourceUrl))
    }

    override fun toTop(rssSource: RssSource) {
        viewModel.topSource(rssSource)
    }

    override fun edit(rssSource: RssSource) {
        startActivity<RssSourceEditActivity>(Pair("data", rssSource.sourceUrl))
    }

    override fun del(rssSource: RssSource) {
        viewModel.del(rssSource)
    }
}