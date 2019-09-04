package io.legado.app.ui.explore

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import kotlinx.android.synthetic.main.activity_explore_show.*

class ExploreShowActivity : BaseActivity(R.layout.activity_explore_show) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("exploreName")?.let {
            title_bar.title = it
        }
        initRecyclerView()
    }

    private fun initRecyclerView() {

    }

    private fun initData() {
        val sourceUrl = intent.getStringExtra("sourceUrl")
        val exploreUrl = intent.getStringExtra("exploreUrl")
    }

}