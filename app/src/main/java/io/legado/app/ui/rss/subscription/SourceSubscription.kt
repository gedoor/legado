package io.legado.app.ui.rss.subscription

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivitySourceSubBinding

class SourceSubscription : BaseActivity<ActivitySourceSubBinding>() {

    override fun getViewBinding(): ActivitySourceSubBinding {
        return ActivitySourceSubBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {

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

    private fun editSubscription() {

    }


}