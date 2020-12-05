package io.legado.app.ui.rss.subscription

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isGone
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.RuleSub
import io.legado.app.databinding.ActivityRuleSubBinding
import io.legado.app.databinding.DialogSourceSubEditBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportBookSourceActivity
import io.legado.app.ui.association.ImportRssSourceActivity
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity

class RuleSubActivity : BaseActivity<ActivityRuleSubBinding>(),
    RuleSubAdapter.Callback {

    private lateinit var adapter: RuleSubAdapter
    private var liveData: LiveData<List<RuleSub>>? = null

    override fun getViewBinding(): ActivityRuleSubBinding {
        return ActivityRuleSubBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_subscription, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> {
                val order = App.db.ruleSubDao().maxOrder + 1
                editSubscription(RuleSub(customOrder = order))
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        adapter = RuleSubAdapter(this, this)
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initData() {
        liveData?.removeObservers(this)
        liveData = App.db.ruleSubDao().observeAll()
        liveData?.observe(this) {
            binding.tvEmptyMsg.isGone = it.isNotEmpty()
            adapter.setItems(it)
        }
    }

    override fun openSubscription(ruleSub: RuleSub) {
        when (ruleSub.type) {
            RuleSub.Type.RssSource.ordinal -> {
                startActivity<ImportRssSourceActivity>("source" to ruleSub.url)
            }
            else -> {
                startActivity<ImportBookSourceActivity>("source" to ruleSub.url)
            }
        }
    }

    override fun editSubscription(ruleSub: RuleSub) {
        alert(R.string.rule_subscription) {
            val alertBinding = DialogSourceSubEditBinding.inflate(layoutInflater).apply {
                when (ruleSub.type) {
                    RuleSub.Type.RssSource.ordinal -> rgType.check(R.id.rb_rss_source)
                    else -> rgType.check(R.id.rb_book_source)
                }
                etName.setText(ruleSub.name)
                etUrl.setText(ruleSub.url)
            }
            customView = alertBinding.root
            okButton {
                when (alertBinding.rgType.checkedRadioButtonId) {
                    R.id.rb_rss_source -> ruleSub.setType(RuleSub.Type.RssSource)
                    else -> ruleSub.setType(RuleSub.Type.BookSource)
                }
                ruleSub.name = alertBinding.etName.text?.toString() ?: ""
                ruleSub.url = alertBinding.etUrl.text?.toString() ?: ""
                launch(IO) {
                    App.db.ruleSubDao().insert(ruleSub)
                }
            }
            cancelButton()
        }.show()
    }

    override fun delSubscription(ruleSub: RuleSub) {
        launch(IO) {
            App.db.ruleSubDao().delete(ruleSub)
        }
    }

    override fun updateSourceSub(vararg ruleSub: RuleSub) {
        launch(IO) {
            App.db.ruleSubDao().update(*ruleSub)
        }
    }

    override fun upOrder() {
        launch(IO) {
            val sourceSubs = App.db.ruleSubDao().all
            for ((index: Int, ruleSub: RuleSub) in sourceSubs.withIndex()) {
                ruleSub.customOrder = index + 1
            }
            App.db.ruleSubDao().update(*sourceSubs.toTypedArray())
        }
    }

}