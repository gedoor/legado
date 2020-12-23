package io.legado.app.ui.main.rss

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.FragmentRssBinding
import io.legado.app.databinding.ItemRssBinding
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.ui.rss.favorites.RssFavoritesActivity
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.rss.source.manage.RssSourceActivity
import io.legado.app.ui.rss.source.manage.RssSourceViewModel
import io.legado.app.ui.rss.subscription.RuleSubActivity
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
    private lateinit var searchView: SearchView
    override val viewModel: RssSourceViewModel
        get() = getViewModel(RssSourceViewModel::class.java)
    private var liveRssData: LiveData<List<RssSource>>? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        searchView = binding.titleBar.findViewById(R.id.search_view)
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
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

    private fun initSearchView() {
        ATH.setTint(searchView, primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.rss)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                initData(newText ?: "")
                return false
            }
        })
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        adapter = RssAdapter(requireContext(), this)
        binding.recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemRssBinding.inflate(layoutInflater, it, false).apply {
                tvName.setText(R.string.rule_subscription)
                ivIcon.setImageResource(R.mipmap.ic_launcher)
                root.onClick {
                    startActivity<RuleSubActivity>()
                }
            }
        }
    }

    private fun initData(searchKey: String = "") {
        liveRssData?.removeObservers(this)
        liveRssData = App.db.rssSourceDao.liveEnabled(searchKey).apply {
            observe(viewLifecycleOwner, {
                adapter.setItems(it)
            })
        }
    }

    override fun openRss(rssSource: RssSource) {
        if (rssSource.singleUrl) {
            startActivity<ReadRssActivity>(
                Pair("title", rssSource.sourceName),
                Pair("origin", rssSource.sourceUrl)
            )
        } else {
            startActivity<RssSortActivity>(Pair("url", rssSource.sourceUrl))
        }
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