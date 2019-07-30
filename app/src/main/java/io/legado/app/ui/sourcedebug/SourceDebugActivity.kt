package io.legado.app.ui.sourcedebug

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.model.WebBook
import io.legado.app.model.webbook.SourceDebug
import io.legado.app.utils.getViewModel
import io.legado.app.utils.isAbsUrl
import kotlinx.android.synthetic.main.activity_source_debug.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class SourceDebugActivity : BaseActivity<AndroidViewModel>(), SourceDebug.Callback {

    override val viewModel: AndroidViewModel
        get() = getViewModel(AndroidViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_source_debug

    private lateinit var adapter: SourceDebugAdapter
    private var bookSource: BookSource? = null

    override fun onActivityCreated(viewModel: AndroidViewModel, savedInstanceState: Bundle?) {
        launch(IO) {
            intent.getStringExtra("key")?.let {
                bookSource = App.db.bookSourceDao().findByKey(it)
            }
        }
        initRecyclerView()
        initSearchView()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        adapter = SourceDebugAdapter()
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
        rotate_loading.loadingColor = ThemeStore.accentColor(this)
    }

    private fun initSearchView() {
        search_view.visibility = View.VISIBLE
        search_view.onActionViewExpanded()
        search_view.isSubmitButtonEnabled = true
        search_view.queryHint = getString(R.string.search_book_key)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                startSearch(query ?: "我的")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun startSearch(key: String) {
        adapter.logList.clear()
        adapter.notifyDataSetChanged()
        bookSource?.let {
            SourceDebug.debugSource = it.bookSourceUrl
            rotate_loading.visibility = View.VISIBLE
            if (key.isAbsUrl()) {
                val book = Book()
                book.origin = it.bookSourceUrl
                book.bookUrl = key
                SourceDebug.printLog(it.bookSourceUrl, 1, "开始访问$key")
                SourceDebug(WebBook(it), this)
                    .infoDebug(book)
            } else {
                SourceDebug.printLog(it.bookSourceUrl, 1, "开始搜索关键字$key")
                SourceDebug(WebBook(it), this)
                    .searchDebug(key)
            }
        }
    }

    override fun printLog(state: Int, msg: String) {
        launch {
            synchronized(this) {
                adapter.logList.add(msg)
                adapter.notifyItemChanged(adapter.logList.size - 1)
                if (state == -1 || state == 1000) {
                    rotate_loading.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        SourceDebug.debugSource = null
        super.onDestroy()
    }
}