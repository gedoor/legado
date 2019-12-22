package io.legado.app.ui.download

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.Book
import kotlinx.android.synthetic.main.activity_download.*


class DownloadActivity : BaseActivity(R.layout.activity_download) {

    lateinit var adapter: DownloadAdapter
    private var bookshelfLiveData: LiveData<List<Book>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initLiveData()
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = DownloadAdapter(this)
        recycler_view.adapter = adapter
    }

    private fun initLiveData() {
        bookshelfLiveData?.removeObservers(this)
        bookshelfLiveData?.observe(this, Observer {

        })
    }
}