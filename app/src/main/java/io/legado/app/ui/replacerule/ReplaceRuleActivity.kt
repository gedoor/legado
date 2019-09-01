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
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_replace_rule.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast


class ReplaceRuleActivity : VMBaseActivity<ReplaceRuleViewModel>(R.layout.activity_replace_rule) {
    override val viewModel: ReplaceRuleViewModel
        get() = getViewModel(ReplaceRuleViewModel::class.java)
    private lateinit var adapter: ReplaceRuleAdapter
    private var rulesLiveData: LiveData<PagedList<ReplaceRule>>? = null
    private var allEnabled = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initDataObservers()
        initSwipeToDelete()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_rule, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_replace_rule)
        rv_replace_rule.layoutManager = LinearLayoutManager(this)
        adapter = ReplaceRuleAdapter(this)
        adapter.onClickListener = object : ReplaceRuleAdapter.OnClickListener {
            override fun update(rule: ReplaceRule) {
                doAsync {
                    App.db.replaceRuleDao().update(rule)
                    updateEnableAll()
                }
            }

            override fun delete(rule: ReplaceRule) {
                doAsync {
                    App.db.replaceRuleDao().delete(rule)
                    updateEnableAll()
                }
            }

            override fun edit(rule: ReplaceRule) {
                doAsync {
                    App.db.replaceRuleDao().enableAll(!allEnabled)
                    allEnabled = !allEnabled
                }

                toast("Edit function not implemented!")
            }
        }
        rv_replace_rule.adapter = adapter
        rv_replace_rule.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
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

    private fun initSwipeToDelete() {
        ItemTouchHelper(object : ItemTouchHelper.Callback() {

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                toast("You swiped the item!")
            }
        }).attachToRecyclerView(rv_replace_rule)

    }
}