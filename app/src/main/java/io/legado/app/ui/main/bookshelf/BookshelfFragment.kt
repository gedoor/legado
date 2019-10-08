package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getViewModel
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.startActivity
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_tab_layout.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity

class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    SearchView.OnQueryTextListener,
    GroupManageDialog.CallBack,
    BookshelfAdapter.CallBack {

    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)

    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private val bookGroups = mutableListOf<BookGroup>()

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
            R.id.menu_group_manage -> GroupManageDialog()
                .show(childFragmentManager, "groupManageDialog")
            R.id.menu_add_local -> {
            }
            R.id.menu_add_url -> {
            }
            R.id.menu_arrange_bookshelf -> {
            }
        }
    }

    override val groupSize: Int
        get() = bookGroups.size

    override fun getGroup(position: Int): BookGroup {
        return bookGroups[position]
    }

    private fun initRecyclerView() {
        tab_layout.isTabIndicatorFullWidth = false
        tab_layout.tabMode = TabLayout.MODE_SCROLLABLE
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
            synchronized(this) {
                bookGroups.clear()
                bookGroups.add(AppConst.bookGroupAll)
                if (AppConst.bookGroupLocalShow) {
                    bookGroups.add(AppConst.bookGroupLocal)
                }
                if (AppConst.bookGroupAudioShow) {
                    bookGroups.add(AppConst.bookGroupAudio)
                }
                bookGroups.addAll(it)
                view_pager_bookshelf.adapter?.notifyDataSetChanged()
            }
        })
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        context?.startActivity<SearchActivity>(Pair("key", query))
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun upGroup() {
        synchronized(this) {
            bookGroups.remove(AppConst.bookGroupLocal)
            bookGroups.remove(AppConst.bookGroupAudio)
            if (getPrefBoolean("bookGroupAudio", true)) {
                bookGroups.add(1, AppConst.bookGroupAudio)
            }
            if (getPrefBoolean("bookGroupLocal", true)) {
                bookGroups.add(1, AppConst.bookGroupLocal)
            }
            view_pager_bookshelf.adapter?.notifyDataSetChanged()
        }
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