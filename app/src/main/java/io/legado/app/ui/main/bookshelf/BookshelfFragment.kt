package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.Bus
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.*
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity

class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    SearchView.OnQueryTextListener,
    BookGroupAdapter.CallBack {

    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)

    private lateinit var booksAdapter: BooksAdapter
    private lateinit var bookGroupAdapter: BookGroupAdapter
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private var position = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initBookGroupData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_search -> startActivity<SearchActivity>()
            R.id.menu_bookshelf_layout -> selectBookshelfLayout()
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(view_pager_bookshelf)
        rv_book_group.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        bookGroupAdapter = BookGroupAdapter(requireContext(), this)
        rv_book_group.adapter = bookGroupAdapter
        view_pager_bookshelf.adapter = BookshelfAdapter(this)
        view_pager_bookshelf.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                this@BookshelfFragment.position = position
            }
        })
        observeEvent<String>(Bus.UP_BOOK) { booksAdapter.notification(it) }
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = App.db.bookGroupDao().liveDataAll()
        bookGroupLiveData?.observe(viewLifecycleOwner, Observer {
            mutableListOf(
                BookGroup(-1, "全部"),
                BookGroup(-2, "本地"),
                BookGroup(-3, "音频")
            ).apply {
                addAll(it)
            }.let {
                bookGroupAdapter.setItems(it)
            }
        })
    }

    override fun open(bookGroup: BookGroup) {
        when (bookGroup.groupId) {
            -10 -> showGroupInputDialog()
            else -> context
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        context?.startActivity<SearchActivity>(Pair("key", query))
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    @SuppressLint("InflateParams")
    private fun showGroupInputDialog() {
        alert(title = "新建分组") {
            var editText: EditText? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view.apply {
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

    private fun selectBookshelfLayout() {
        selector(
            title = "选择书架布局",
            items = resources.getStringArray(R.array.bookshelf_layout).toList()
        ) { _, index ->
            putPrefInt("bookshelf", index)
        }
    }
}