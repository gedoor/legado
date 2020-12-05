package io.legado.app.ui.rss.subscription

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.SourceSub
import io.legado.app.databinding.ActivitySourceSubBinding
import io.legado.app.databinding.DialogSourceSubEditBinding
import io.legado.app.lib.dialogs.alert

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
            R.id.menu_add -> editSubscription(SourceSub())
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        adapter = SourceSubAdapter(this, this)
        binding.recyclerView.adapter = adapter
    }

    private fun initData() {
        liveData?.removeObservers(this)
        liveData = App.db.sourceSubDao().observeAll()
        liveData?.observe(this) {
            adapter.setItems(it)
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
                App.db.sourceSubDao().insert(sourceSub)
            }
            cancelButton()
        }.show()
    }

    override fun delSubscription(sourceSub: SourceSub) {

    }
}