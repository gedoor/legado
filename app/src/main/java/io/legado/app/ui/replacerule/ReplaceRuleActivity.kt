package io.legado.app.ui.replacerule

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModel
import io.legado.app.utils.splitNotBlank
import kotlinx.android.synthetic.main.activity_replace_rule.*
import org.jetbrains.anko.doAsync


class ReplaceRuleActivity : VMBaseActivity<ReplaceRuleViewModel>(R.layout.activity_replace_rule),
    ReplaceRuleAdapter.CallBack {
    override val viewModel: ReplaceRuleViewModel
        get() = getViewModel(ReplaceRuleViewModel::class.java)

    private lateinit var adapter: ReplaceRuleAdapter
    private var rulesLiveData: LiveData<PagedList<ReplaceRule>>? = null
    private var allEnabled = false
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initDataObservers()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_rule, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu?.findItem(R.id.menu_group)?.subMenu
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_replace_rule ->
                ReplaceEditDialog().show(supportFragmentManager, "replaceNew")
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = ReplaceRuleAdapter(this, this)
        recycler_view.adapter = adapter
        recycler_view.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initDataObservers() {
        rulesLiveData?.removeObservers(this)
        rulesLiveData = LivePagedListBuilder(App.db.replaceRuleDao().observeAll(), 30).build()
        rulesLiveData?.observe(this, Observer<PagedList<ReplaceRule>> { adapter.submitList(it) })

        App.db.replaceRuleDao().liveGroup().observe(this, Observer {
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

    private fun updateEnableAll() {
        doAsync {
            App.db.replaceRuleDao().summary.let {
                allEnabled = it == 0
            }
        }
    }

    override fun update(rule: ReplaceRule) {
        viewModel.update(rule)
    }

    override fun delete(rule: ReplaceRule) {
        viewModel.delete(rule)
    }

    override fun edit(rule: ReplaceRule) {
        ReplaceEditDialog
            .newInstance(rule.id)
            .show(supportFragmentManager, "editReplace")
    }

    override fun toTop(rule: ReplaceRule) {
        viewModel.toTop(rule)
    }
}