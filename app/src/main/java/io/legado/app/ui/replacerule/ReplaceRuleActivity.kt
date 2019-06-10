package io.legado.app.ui.replacerule

import android.os.Bundle
import android.util.Log
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
import io.legado.app.base.BaseActivity
import io.legado.app.constant.AppConst.APP_TAG
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_replace_rule.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast


class ReplaceRuleActivity : BaseActivity<ReplaceRuleViewModel>() {
    override val viewModel: ReplaceRuleViewModel
        get() = getViewModel(ReplaceRuleViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_replace_rule
    private lateinit var adapter: ReplaceRuleAdapter
    private var rulesLiveData: LiveData<PagedList<ReplaceRule>>? = null
    private var allEnabled = false

    override fun onViewModelCreated(viewModel: ReplaceRuleViewModel, savedInstanceState: Bundle?) {
        initRecyclerView()
        initDataObservers()
        initSwipeToDelete()
    }

    private fun initRecyclerView() {
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
                    Log.e(APP_TAG, it.toString())
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
                // TODO()
                // remove((viewHolder as TodoViewHolder).todo)
            }
        }).attachToRecyclerView(rv_replace_rule)

    }
}