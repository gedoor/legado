package io.legado.app.ui.rss.source.manage

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.RssSource
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.splitNotBlank
import kotlinx.android.synthetic.main.activity_rss_source.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity


class RssSourceActivity : VMBaseActivity<RssSourceViewModel>(R.layout.activity_rss_source),
    RssSourceAdapter.CallBack {

    override val viewModel: RssSourceViewModel
        get() = getViewModel(RssSourceViewModel::class.java)

    private lateinit var adapter: RssSourceAdapter
    private var sourceLiveData: LiveData<List<RssSource>>? = null
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        initRecyclerView()
        initSearchView()
        initLiveDataGroup()
        initLiveDataSource()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_source, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu?.findItem(R.id.menu_group)?.subMenu
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> startActivity<RssSourceEditActivity>()
            R.id.menu_select_all -> adapter.selectAll()
            R.id.menu_revert_selection -> adapter.revertSelection()
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelectionIds())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelectionIds())
            R.id.menu_del_selection -> viewModel.delSelection(adapter.getSelectionIds())
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
        adapter = RssSourceAdapter(this, this)
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initSearchView() {
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.search_rss_source)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun initLiveDataGroup() {
        App.db.rssSourceDao().liveGroup().observe(this, Observer {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(",", ";"))
            }
            upGroupMenu()
        })
    }

    private fun upGroupMenu() {
        groupMenu?.removeGroup(R.id.source_group)
        groups.map {
            groupMenu?.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
        }
    }

    private fun initLiveDataSource(key: String? = null) {
        sourceLiveData?.removeObservers(this)
        sourceLiveData =
            if (key.isNullOrBlank()) {
                App.db.rssSourceDao().liveAll()
            } else {
                App.db.rssSourceDao().liveSearch("%$key%")
            }
        sourceLiveData?.observe(this, Observer {
            val diffResult = DiffUtil
                .calculateDiff(DiffCallBack(adapter.getItems(), it))
            adapter.setItemsNoNotify(it)
            diffResult.dispatchUpdatesTo(adapter)
        })
    }

    override fun del(source: RssSource) {
        viewModel.del(source)
    }

    override fun edit(source: RssSource) {
        startActivity<RssSourceEditActivity>(Pair("data", source.sourceUrl))
    }

    override fun update(vararg source: RssSource) {
        viewModel.update(*source)
    }

    override fun toTop(source: RssSource) {
        viewModel.topSource(source)
    }

    override fun upOrder() {
        viewModel.upOrder()
    }

}