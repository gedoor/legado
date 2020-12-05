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

class SourceSubscriptionActivity : BaseActivity<ActivitySourceSubBinding>() {

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
            R.id.menu_add -> editSubscription()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        adapter = SourceSubAdapter(this)
        binding.recyclerView.adapter = adapter
    }

    private fun initData() {
        liveData?.removeObservers(this)
        liveData = App.db.sourceSubDao().observeAll()
        liveData?.observe(this) {
            adapter.setItems(it)
        }
    }

    private fun editSubscription() {

    }


}