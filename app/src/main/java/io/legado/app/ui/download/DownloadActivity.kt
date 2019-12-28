package io.legado.app.ui.download

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.service.help.Download
import kotlinx.android.synthetic.main.activity_download.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


class DownloadActivity : BaseActivity(R.layout.activity_download) {

    lateinit var adapter: DownloadAdapter
    private var bookshelfLiveData: LiveData<List<Book>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initLiveData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.download, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_download_start -> launch(IO) {
                App.db.bookDao().webBooks.forEach { book ->
                    Download.start(
                        this@DownloadActivity,
                        book.bookUrl,
                        book.durChapterIndex,
                        book.totalChapterNum
                    )
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = DownloadAdapter(this)
        recycler_view.adapter = adapter
    }

    private fun initLiveData() {
        bookshelfLiveData?.removeObservers(this)
        bookshelfLiveData = App.db.bookDao().observeDownload()
        bookshelfLiveData?.observe(this, Observer {
            adapter.setItems(it)
        })
    }
}