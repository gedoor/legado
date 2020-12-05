package io.legado.app.ui.rss.subscription

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.SourceSub
import io.legado.app.databinding.ActivitySourceSubBinding
import io.legado.app.databinding.DialogSourceSubEditBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportBookSourceActivity
import io.legado.app.ui.association.ImportRssSourceActivity
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity

class SourceSubActivity : BaseActivity<ActivitySourceSubBinding>(),
    SourceSubAdapter.Callback {

    private lateinit var adapter: SourceSubAdapter
    private var liveData: LiveData<List<SourceSub>>? = null

    override fun getViewBinding(): ActivitySourceSubBinding {
        return ActivitySourceSubBinding.inflate(layoutInflater)
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
                val order = App.db.sourceSubDao().maxOrder + 1
                editSubscription(SourceSub(customOrder = order))
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        adapter = SourceSubAdapter(this, this)
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initData() {
        liveData?.removeObservers(this)
        liveData = App.db.sourceSubDao().observeAll()
        liveData?.observe(this) {
            adapter.setItems(it)
        }
    }

    override fun openSubscription(sourceSub: SourceSub) {
        when (sourceSub.type) {
            SourceSub.Type.RssSource.ordinal -> {
                startActivity<ImportRssSourceActivity>("source" to sourceSub.url)
            }
            else -> {
                startActivity<ImportBookSourceActivity>("source" to sourceSub.url)
            }
        }
    }

    override fun editSubscription(sourceSub: SourceSub) {
        alert(R.string.source_subscription) {
            val alertBinding = DialogSourceSubEditBinding.inflate(layoutInflater).apply {
                when (sourceSub.type) {
                    SourceSub.Type.RssSource.ordinal -> rgType.check(R.id.rb_rss_source)
                    else -> rgType.check(R.id.rb_book_source)
                }
                etName.setText(sourceSub.name)
                etUrl.setText(sourceSub.url)
            }
            customView = alertBinding.root
            okButton {
                when (alertBinding.rgType.checkedRadioButtonId) {
                    R.id.rb_rss_source -> sourceSub.setType(SourceSub.Type.RssSource)
                    else -> sourceSub.setType(SourceSub.Type.BookSource)
                }
                sourceSub.name = alertBinding.etName.text?.toString() ?: ""
                sourceSub.url = alertBinding.etUrl.text?.toString() ?: ""
                launch(IO) {
                    App.db.sourceSubDao().insert(sourceSub)
                }
            }
            cancelButton()
        }.show()
    }

    override fun delSubscription(sourceSub: SourceSub) {
        launch(IO) {
            App.db.sourceSubDao().delete(sourceSub)
        }
    }

    override fun updateSourceSub(vararg sourceSub: SourceSub) {
        launch(IO) {
            App.db.sourceSubDao().update(*sourceSub)
        }
    }

    override fun upOrder() {
        launch(IO) {
            val sourceSubs = App.db.sourceSubDao().all
            for ((index: Int, sourceSub: SourceSub) in sourceSubs.withIndex()) {
                sourceSub.customOrder = index + 1
            }
            App.db.sourceSubDao().update(*sourceSubs.toTypedArray())
        }
    }

}