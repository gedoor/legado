package io.legado.app.ui.main.booksource

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ItemTouchCallback
import io.legado.app.ui.sourceedit.SourceEditActivity
import kotlinx.android.synthetic.main.fragment_book_source.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity

class BookSourceFragment : BaseFragment(R.layout.fragment_book_source), BookSourceAdapter.CallBack {

    private lateinit var adapter: BookSourceAdapter
    private var bookSourceLiveDate: LiveData<PagedList<BookSource>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initDataObservers()
        initSearchView()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.book_source, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.action_add_book_source -> {
                context?.startActivity<SourceEditActivity>()
            }
        }
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        adapter = BookSourceAdapter()
        adapter.callBack = this
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter.itemTouchCallbackListener
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initSearchView() {
        search_view.visibility = View.VISIBLE
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.search_book_source)
        search_view.clearFocus()
    }

    private fun initDataObservers() {
        bookSourceLiveDate?.removeObservers(viewLifecycleOwner)
        bookSourceLiveDate = LivePagedListBuilder(App.db.bookSourceDao().observeAll(), 30).build()
        bookSourceLiveDate?.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
    }

    override fun del(bookSource: BookSource) {
        GlobalScope.launch { App.db.bookSourceDao().delete(bookSource) }
    }

    override fun update(bookSource: BookSource) {
        GlobalScope.launch { App.db.bookSourceDao().update(bookSource) }
    }

    override fun update(vararg bookSource: BookSource) {
        GlobalScope.launch { App.db.bookSourceDao().update(*bookSource) }
    }

    override fun edit(bookSource: BookSource) {
        context?.let { it.startActivity<SourceEditActivity>(Pair("data", bookSource.origin)) }
    }
}