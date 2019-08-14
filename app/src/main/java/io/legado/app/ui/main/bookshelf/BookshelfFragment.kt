package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.Bus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.bookinfo.BookInfoActivity
import io.legado.app.ui.bookshelf.BookshelfActivity
import io.legado.app.ui.read.ReadActivity
import io.legado.app.ui.search.SearchActivity
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_edittext.view.*
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor

class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    SearchView.OnQueryTextListener,
    BookGroupAdapter.CallBack,
    BookshelfAdapter.CallBack {

    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)

    private lateinit var bookshelfAdapter: BookshelfAdapter
    private lateinit var bookGroupAdapter: BookGroupAdapter
    private var bookGroupLiveData: LiveData<PagedList<BookGroup>>? = null
    private var bookshelfLiveData: LiveData<PagedList<Book>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initBookGroupData()
        initBookshelfData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.action_search -> startActivity<SearchActivity>()
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_bookshelf)
        refresh_layout.setColorSchemeColors(accentColor)
        refresh_layout.setOnRefreshListener {
            refresh_layout.isRefreshing = false
            viewModel.upChapterList()
        }
        tv_recent_reading.textColor = accentColor
        rv_book_group.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        bookGroupAdapter = BookGroupAdapter()
        rv_book_group.adapter = bookGroupAdapter
        bookGroupAdapter.callBack = this
        rv_bookshelf.layoutManager = LinearLayoutManager(context)
        rv_bookshelf.addItemDecoration(DividerItemDecoration(rv_bookshelf.context, LinearLayoutManager.VERTICAL))
        bookshelfAdapter = BookshelfAdapter()
        rv_bookshelf.adapter = bookshelfAdapter
        bookshelfAdapter.callBack = this
        observeEvent<String>(Bus.UP_BOOK) { bookshelfAdapter.notification(it) }
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
            -10 -> showGroupInputDialog()
            else -> context?.startActivity<BookshelfActivity>(Pair("data", bookGroup))
        }
    }

    override fun open(book: Book) {
        context?.startActivity<ReadActivity>(Pair("bookUrl", book.bookUrl))
    }

    override fun openBookInfo(book: Book) {
        context?.startActivity<BookInfoActivity>(Pair("bookUrl", book.bookUrl))
    }

    override fun search() {

    }

    override fun isUpdate(bookUrl: String): Boolean {
        return bookUrl in viewModel.updateList
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        context?.startActivity<SearchActivity>(Pair("key", query))
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    private fun showGroupInputDialog() {
        alert(title = "新建分组") {
            var editText: EditText? = null

            customView {
                layoutInflater.inflate(R.layout.dialog_edittext, null).apply {
                    editText = edit_view.apply {
                        ATH.applyAccentTint(this)
                        hint = "分组名称"
                    }
                }
            }

            yesButton {
                viewModel.saveBookGroup(editText?.text?.toString())
            }

            noButton()

        }.show().applyTint().requestInputMethod()
    }

}