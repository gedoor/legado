package io.legado.app.ui.main.booksource

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.data.entities.BookSource
import kotlinx.android.synthetic.main.fragment_book_source.*
import kotlinx.android.synthetic.main.view_titlebar.*

class BookSourceFragment : BaseFragment(R.layout.fragment_book_source) {

    private lateinit var adapter: BookSourceAdapter
    private var bookSourceLiveDate: LiveData<PagedList<BookSource>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initDataObservers()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.book_source, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {

    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context)
        adapter = BookSourceAdapter()
        recycler_view.adapter = adapter
    }

    private fun initDataObservers() {
        bookSourceLiveDate?.removeObservers(viewLifecycleOwner)
        bookSourceLiveDate = LivePagedListBuilder(App.db.sourceDao().observeAll(), 30).build()
        bookSourceLiveDate?.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
    }
}