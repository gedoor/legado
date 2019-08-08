package io.legado.app.ui.booksource

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.ui.sourceedit.SourceEditActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_book_source.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult

class BookSourceActivity : VMBaseActivity<BookSourceViewModel>(R.layout.activity_book_source),
    BookSourceAdapter.CallBack,
    SearchView.OnQueryTextListener {
    override val viewModel: BookSourceViewModel
        get() = getViewModel(BookSourceViewModel::class.java)

    private lateinit var adapter: BookSourceAdapter
    private var bookSourceLiveDate: LiveData<PagedList<BookSource>>? = null
    private val qrRequestCode = 101

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        initRecyclerView()
        initDataObserve()
        initSearchView()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_source, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_book_source -> {
                this.startActivity<SourceEditActivity>()
            }
            R.id.action_import_book_source_qr -> {
                this.startActivityForResult<QrCodeActivity>(qrRequestCode)
            }
            R.id.action_select_all -> {
                launch(IO) {
                    val isEnableList =
                        App.db.bookSourceDao().searchIsEnable("%${search_view.query}%")
                    if (isEnableList.contains(false)) {
                        App.db.bookSourceDao().enableAllSearch("%${search_view.query}%", "1")
                    } else {
                        App.db.bookSourceDao().enableAllSearch("%${search_view.query}%", "0")
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
        adapter = BookSourceAdapter()
        adapter.callBack = this
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter.itemTouchCallbackListener
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initSearchView() {
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.search_book_source)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(this)
    }

    private fun initDataObserve(searchKey: String = "") {
        bookSourceLiveDate?.removeObservers(this)
        val dataFactory = App.db.bookSourceDao().observeSearch("%$searchKey%")
        bookSourceLiveDate = LivePagedListBuilder(dataFactory, 2000).build()
        bookSourceLiveDate?.observe(this, Observer { adapter.submitList(it) })
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let {
            initDataObserve(it)
        }
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun upCount(count: Int) {
        search_view.queryHint = getString(R.string.search_book_source_num, count)
    }

    override fun del(bookSource: BookSource) {
        launch(IO) { App.db.bookSourceDao().delete(bookSource) }
    }

    override fun update(bookSource: BookSource) {
        launch(IO) { App.db.bookSourceDao().update(bookSource) }
    }

    override fun update(vararg bookSource: BookSource) {
        launch(IO) { App.db.bookSourceDao().update(*bookSource) }
    }

    override fun edit(bookSource: BookSource) {
        startActivity<SourceEditActivity>(Pair("data", bookSource.bookSourceUrl))
    }

    override fun topSource(bookSource: BookSource) {
        viewModel.topSource(bookSource)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            qrRequestCode -> {
                if (resultCode == RESULT_OK) {
                    data?.getStringExtra("result")?.let {

                    }
                }
            }
        }
    }

    override fun finish() {
        if (search_view.query.isNullOrEmpty()) {
            super.finish()
        } else {
            search_view.setQuery("", true)
        }
    }
}