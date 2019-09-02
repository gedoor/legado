package io.legado.app.ui.replacerule

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import kotlinx.android.synthetic.main.activity_book_source.*
import kotlinx.android.synthetic.main.activity_replace_rule.*
import org.jetbrains.anko.doAsync


class ReplaceRuleActivity : VMBaseActivity<ReplaceRuleViewModel>(R.layout.activity_replace_rule),
    ReplaceRuleAdapter.CallBack {
    override val viewModel: ReplaceRuleViewModel
        get() = getViewModel(ReplaceRuleViewModel::class.java)

    private lateinit var adapter: ReplaceRuleAdapter
    private var rulesLiveData: LiveData<PagedList<ReplaceRule>>? = null
    private var allEnabled = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initDataObservers()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_rule, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_replace_rule ->
                ReplaceEditDialog().show(supportFragmentManager, "replaceNew")
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_replace_rule)
        rv_replace_rule.layoutManager = LinearLayoutManager(this)
        adapter = ReplaceRuleAdapter(this, this)
        rv_replace_rule.adapter = adapter
        rv_replace_rule.addItemDecoration(
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

    }
}