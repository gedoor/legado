package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_title_bar.*

class BookshelfFragment : BaseFragment(R.layout.fragment_bookshelf) {

    private lateinit var recentReadAdapter: RecentReadAdapter
    private lateinit var bookGroupAdapter: BookGroupAdapter
    private var bookGroupLiveData: LiveData<PagedList<BookGroup>>? = null
    private var recentReadLiveData: LiveData<PagedList<Book>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initBookGroupData()
        initRecentReadData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.bookshelf, menu)
    }

    private fun initRecyclerView() {
        rv_bookshelf.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        bookGroupAdapter = BookGroupAdapter()
        rv_bookshelf.adapter = bookGroupAdapter
        rv_read_books.layoutManager = LinearLayoutManager(context)
        recentReadAdapter = RecentReadAdapter()
        rv_read_books.adapter = recentReadAdapter
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = LivePagedListBuilder(App.db.bookGroupDao().observeAll(), 10).build()
        bookGroupLiveData?.observe(viewLifecycleOwner, Observer { bookGroupAdapter.submitList(it) })
    }

    private fun initRecentReadData() {
        recentReadLiveData?.removeObservers(viewLifecycleOwner)
        recentReadLiveData = LivePagedListBuilder(App.db.bookDao().recentRead(), 20).build()
        recentReadLiveData?.observe(viewLifecycleOwner, Observer { recentReadAdapter.submitList(it) })
    }

}