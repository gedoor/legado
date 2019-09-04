package io.legado.app.ui.explore

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.BaseActivity
import kotlinx.android.synthetic.main.activity_explore_show.*

class ExploreShowActivity : BaseActivity(R.layout.activity_explore_show),
    ExploreShowAdapter.CallBack {

    private lateinit var adapter: ExploreShowAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("exploreName")?.let {
            title_bar.title = it
        }
        initRecyclerView()
    }

    private fun initRecyclerView() {
        adapter = ExploreShowAdapter(this, this)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        recycler_view.adapter = adapter
    }

    private fun initData() {
        val sourceUrl = intent.getStringExtra("sourceUrl")
        val exploreUrl = intent.getStringExtra("exploreUrl")
    }

    override fun showBookInfo(name: String, author: String) {

    }
}