package io.legado.app.ui.download

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseActivity
import kotlinx.android.synthetic.main.activity_download.*


class DownloadActivity : BaseActivity(R.layout.activity_download) {

    lateinit var adapter: DownloadAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = DownloadAdapter(this)
        recycler_view.adapter = adapter
    }

}