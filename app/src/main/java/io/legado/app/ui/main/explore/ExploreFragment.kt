package io.legado.app.ui.main.explore

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.FragmentExploreBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.book.explore.ExploreShowActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.utils.cnCompare
import io.legado.app.utils.getViewModel
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 发现界面
 */
class ExploreFragment : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore),
    ExploreAdapter.CallBack {
    override val viewModel: ExploreViewModel
        get() = getViewModel(ExploreViewModel::class.java)
    private val binding by viewBinding(FragmentExploreBinding::bind)
    private lateinit var adapter: ExploreAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var searchView: SearchView
    private val groups = linkedSetOf<String>()
    private var liveGroup: LiveData<List<String>>? = null
    private var liveExplore: LiveData<List<BookSource>>? = null
    private var groupsMenu: SubMenu? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        searchView = binding.titleBar.findViewById(R.id.search_view)
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
        initRecyclerView()
        initGroupData()
        initExploreData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        super.onCompatCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_explore, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
    }

    private fun initSearchView() {
        ATH.setTint(searchView, primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.screen_find)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                initExploreData(newText)
                return false
            }
        })
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.rvFind)
        linearLayoutManager = LinearLayoutManager(context)
        binding.rvFind.layoutManager = linearLayoutManager
        adapter = ExploreAdapter(requireContext(), this, this)
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

    private fun initGroupData() {
        liveGroup?.removeObservers(viewLifecycleOwner)
        liveGroup = App.db.bookSourceDao.liveExploreGroup()
        liveGroup?.observe(viewLifecycleOwner, {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
            }
            upGroupsMenu()
        })
    }

    private fun initExploreData(searchKey: String? = null) {
        liveExplore?.removeObservers(viewLifecycleOwner)
        liveExplore = when {
            searchKey.isNullOrBlank() -> {
                App.db.bookSourceDao.liveExplore()
            }
            searchKey.startsWith("group:") -> {
                val key = searchKey.substringAfter("group:")
                App.db.bookSourceDao.liveGroupExplore("%$key%")
            }
            else -> {
                App.db.bookSourceDao.liveExplore("%$searchKey%")
            }
        }
        liveExplore?.observe(viewLifecycleOwner, {
            binding.tvEmptyMsg.isGone = it.isNotEmpty() || searchView.query.isNotEmpty()
            val diffResult = DiffUtil
                .calculateDiff(ExploreDiffCallBack(ArrayList(adapter.getItems()), it))
            adapter.setItems(it)
            diffResult.dispatchUpdatesTo(adapter)
        })
    }

    private fun upGroupsMenu() = groupsMenu?.let { subMenu ->
        subMenu.removeGroup(R.id.menu_group_text)
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.forEach {
            subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        if (item.groupId == R.id.menu_group_text) {
            searchView.setQuery("group:${item.title}", true)
        }
    }

    override fun scrollTo(pos: Int) {
        (binding.rvFind.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
    }

    override fun openExplore(sourceUrl: String, title: String, exploreUrl: String) {
        startActivity<ExploreShowActivity>(
            Pair("exploreName", title),
            Pair("sourceUrl", sourceUrl),
            Pair("exploreUrl", exploreUrl)
        )
    }

    override fun editSource(sourceUrl: String) {
        startActivity<BookSourceEditActivity>(Pair("data", sourceUrl))
    }

    override fun toTop(source: BookSource) {
        viewModel.topSource(source)
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