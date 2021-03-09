package io.legado.app.ui.main.rss

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
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
import io.legado.app.utils.cnCompare
import io.legado.app.utils.openUrl
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding


/**
 * 订阅界面
 */
class RssFragment : VMBaseFragment<RssSourceViewModel>(R.layout.fragment_rss),
    RssAdapter.CallBack {
    private val binding by viewBinding(FragmentRssBinding::bind)
    private lateinit var adapter: RssAdapter
    private lateinit var searchView: SearchView
    override val viewModel: RssSourceViewModel
            by viewModels()
    private var liveRssData: LiveData<List<RssSource>>? = null
    private val groups = linkedSetOf<String>()
    private var liveGroup: LiveData<List<String>>? = null
    private var groupsMenu: SubMenu? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        searchView = binding.titleBar.findViewById(R.id.search_view)
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
        initRecyclerView()
        initGroupData()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_rss, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_rss_config -> startActivity<RssSourceActivity>()
            R.id.menu_rss_star -> startActivity<RssFavoritesActivity>()
            else -> if (item.groupId == R.id.menu_group_text) {
                searchView.setQuery(item.title, true)
            }
        }
    }

    private fun upGroupsMenu() = groupsMenu?.let { subMenu ->
        subMenu.removeGroup(R.id.menu_group_text)
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.forEach {
            subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
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
                initData(newText)
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
                ivIcon.setImageResource(R.drawable.image_legado)
                root.setOnClickListener {
                    startActivity<RuleSubActivity>()
                }
            }
        }
    }

    private fun initData(searchKey: String? = null) {
        liveRssData?.removeObservers(this)
        liveRssData = when {
            searchKey.isNullOrEmpty() -> appDb.rssSourceDao.liveEnabled()
            searchKey.startsWith("group:") -> {
                val key = searchKey.substringAfter("group:")
                appDb.rssSourceDao.liveEnabledByGroup("%$key%")
            }
            else -> appDb.rssSourceDao.liveEnabled("%$searchKey%")
        }.apply {
            observe(viewLifecycleOwner, {
                adapter.setItems(it)
            })
        }
    }

    private fun initGroupData() {
        liveGroup?.removeObservers(viewLifecycleOwner)
        liveGroup = appDb.rssSourceDao.liveGroup()
        liveGroup?.observe(viewLifecycleOwner, {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
            }
            upGroupsMenu()
        })
    }

    override fun openRss(rssSource: RssSource) {
        if (rssSource.singleUrl) {
            if (rssSource.sourceUrl.startsWith("http", true)) {
                startActivity<ReadRssActivity> {
                    putExtra("title", rssSource.sourceName)
                    putExtra("origin", rssSource.sourceUrl)
                }
            } else {
                context?.openUrl(rssSource.sourceUrl)
            }
        } else {
            startActivity<RssSortActivity> {
                putExtra("url", rssSource.sourceUrl)
            }
        }
    }

    override fun toTop(rssSource: RssSource) {
        viewModel.topSource(rssSource)
    }

    override fun edit(rssSource: RssSource) {
        startActivity<RssSourceEditActivity> {
            putExtra("data", rssSource.sourceUrl)
        }
    }

    override fun del(rssSource: RssSource) {
        viewModel.del(rssSource)
    }
}