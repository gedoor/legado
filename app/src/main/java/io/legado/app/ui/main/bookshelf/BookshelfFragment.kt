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
import com.google.android.material.tabs.TabLayoutMediator
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.*
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_tab_layout.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity

class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    SearchView.OnQueryTextListener,
    BookGroupAdapter.CallBack,
    BookshelfAdapter.CallBack {

    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)

    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private val bookGroups = mutableListOf<BookGroup>().apply { addAll(AppConst.defaultBookGroups) }

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

    override val groupSize: Int
        get() = bookGroups.size

    override fun getGroup(position: Int): BookGroup {
        return bookGroups[position]
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(view_pager_bookshelf)
        view_pager_bookshelf.adapter = BookshelfAdapter(this, this)
        TabLayoutMediator(tab_layout, view_pager_bookshelf) { tab, position ->
            tab.text = bookGroups[position].groupName
        }.attach()
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = App.db.bookGroupDao().liveDataAll()
        bookGroupLiveData?.observe(viewLifecycleOwner, Observer {
            for (index in AppConst.defaultBookGroups.size until bookGroups.size) {
                bookGroups.removeAt(AppConst.defaultBookGroups.size)
            }
            bookGroups.addAll(it)
            view_pager_bookshelf.adapter?.notifyDataSetChanged()
        })
    }

    override fun onClickGroup(position: Int) {
        view_pager_bookshelf.setCurrentItem(position, false)
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