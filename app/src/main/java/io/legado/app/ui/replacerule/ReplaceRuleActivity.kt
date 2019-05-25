package io.legado.app.ui.replacerule

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.ReplaceRule
import kotlinx.android.synthetic.main.activity_replace_rule.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper


class ReplaceRuleActivity : AppCompatActivity() {
    private lateinit var adapter: ReplaceRuleAdapter
    private var rulesLiveData: LiveData<PagedList<ReplaceRule>>? = null
    private var allEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replace_rule)
        rv_replace_rule.layoutManager = LinearLayoutManager(this)
        initRecyclerView()
        initDataObservers()
        initSwipeToDelete()
    }

    private fun initRecyclerView() {
        rv_replace_rule.layoutManager = LinearLayoutManager(this)
        adapter = ReplaceRuleAdapter(this)
        adapter.onClickListener = object: ReplaceRuleAdapter.OnClickListener {
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
                return ItemTouchHelper.Callback.makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
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
                TODO()
                // remove((viewHolder as TodoViewHolder).todo)
            }
        }).attachToRecyclerView(rv_replace_rule)

    }
}