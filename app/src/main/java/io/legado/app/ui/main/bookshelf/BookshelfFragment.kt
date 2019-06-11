package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.bookshelf.BookshelfActivity
import io.legado.app.utils.disableAutoFill
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor

class BookshelfFragment : BaseFragment(R.layout.fragment_bookshelf), BookGroupAdapter.CallBack {

    private lateinit var bookshelfAdapter: BookshelfAdapter
    private lateinit var bookGroupAdapter: BookGroupAdapter
    private var bookGroupLiveData: LiveData<PagedList<BookGroup>>? = null
    private var bookshelfLiveData: LiveData<PagedList<Book>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initSearchView()
        initRecyclerView()
        initBookGroupData()
        initBookshelfData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.bookshelf, menu)
    }

    private fun initSearchView() {
        search_view.visibility = View.VISIBLE
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.search_book_key)
        search_view.clearFocus()
    }

    private fun initRecyclerView() {
        refresh_layout.setColorSchemeColors(ThemeStore.accentColor(refresh_layout.context))
        tv_recent_reading.textColor = ThemeStore.accentColor(tv_recent_reading.context)
        rv_book_group.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        bookGroupAdapter = BookGroupAdapter()
        rv_book_group.adapter = bookGroupAdapter
        bookGroupAdapter.callBack = this
        rv_bookshelf.layoutManager = LinearLayoutManager(context)
        rv_bookshelf.addItemDecoration(DividerItemDecoration(rv_bookshelf.context, LinearLayoutManager.VERTICAL))
        bookshelfAdapter = BookshelfAdapter()
        rv_bookshelf.adapter = bookshelfAdapter
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = LivePagedListBuilder(App.db.bookGroupDao().observeAll(), 10).build()
        bookGroupLiveData?.observe(viewLifecycleOwner, Observer { bookGroupAdapter.submitList(it) })
    }

    private fun initBookshelfData() {
        bookshelfLiveData?.removeObservers(viewLifecycleOwner)
        bookshelfLiveData = LivePagedListBuilder(App.db.bookDao().recentRead(), 20).build()
        bookshelfLiveData?.observe(viewLifecycleOwner, Observer { bookshelfAdapter.submitList(it) })
    }

    override fun open(bookGroup: BookGroup) {
        when (bookGroup.groupId) {
            -10 -> context?.let {
                MaterialDialog(it).show {
                    window?.decorView?.disableAutoFill()
                    title(text = "新建分组")
                    input(hint = "分组名称") { _, charSequence ->
                        run {
                            GlobalScope.launch {
                                App.db.bookGroupDao().insert(
                                    BookGroup(
                                        App.db.bookGroupDao().maxId + 1,
                                        charSequence.toString()
                                    )
                                )
                            }
                        }
                    }
                    positiveButton(R.string.ok)
                }
            }
            else -> context?.startActivity<BookshelfActivity>(Pair("data", bookGroup))
        }
    }

}