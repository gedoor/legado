package io.legado.app.ui.main.explore

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.book.explore.ExploreShowActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.startActivity
import kotlinx.android.synthetic.main.fragment_find_book.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.android.synthetic.main.view_title_bar.*
import java.text.Collator


class ExploreFragment : VMBaseFragment<ExploreViewModel>(R.layout.fragment_find_book),
    ExploreAdapter.CallBack {
    override val viewModel: ExploreViewModel
        get() = getViewModel(ExploreViewModel::class.java)

    private lateinit var adapter: ExploreAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val groups = linkedSetOf<String>()
    private var liveGroup: LiveData<List<String>>? = null
    private var liveExplore: LiveData<List<BookSource>>? = null
    private var groupsMenu: SubMenu? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
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
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.isSubmitButtonEnabled = true
        search_view.queryHint = getString(R.string.screen_find)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
        ATH.applyEdgeEffectColor(rv_find)
        rv_find.isEnableScroll = !AppConfig.isEInkMode
        linearLayoutManager = LinearLayoutManager(context)
        rv_find.layoutManager = linearLayoutManager
        adapter = ExploreAdapter(requireContext(), this, this)
        rv_find.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    rv_find.scrollToPosition(0)
                }
            }
        })
    }

    private fun initGroupData() {
        liveGroup?.removeObservers(viewLifecycleOwner)
        liveGroup = App.db.bookSourceDao().liveGroupExplore()
        liveGroup?.observe(viewLifecycleOwner, Observer {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(",", ";"))
            }
            upGroupsMenu()
        })
    }

    private fun initExploreData(key: String? = null) {
        liveExplore?.removeObservers(viewLifecycleOwner)
        liveExplore = if (key.isNullOrBlank()) {
            App.db.bookSourceDao().liveExplore()
        } else {
            App.db.bookSourceDao().liveExplore("%$key%")
        }
        liveExplore?.observe(viewLifecycleOwner, Observer {
            val diffResult = DiffUtil
                .calculateDiff(ExploreDiffCallBack(ArrayList(adapter.getItems()), it))
            adapter.setItems(it)
            diffResult.dispatchUpdatesTo(adapter)
        })
    }

    private fun upGroupsMenu() {
        groupsMenu?.let { subMenu ->
            subMenu.removeGroup(R.id.menu_group_text)
            groups.sortedWith(Collator.getInstance(java.util.Locale.CHINESE))
                .forEach {
                    subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
                }
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        if (item.groupId == R.id.menu_group_text) {
            search_view.setQuery(item.title, true)
        }
    }

    override fun scrollTo(pos: Int) {
        (rv_find.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
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

}