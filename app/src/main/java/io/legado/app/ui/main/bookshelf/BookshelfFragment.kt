package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
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
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.arrange.ArrangeBookActivity
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.download.DownloadActivity
import io.legado.app.ui.importbook.ImportBookActivity
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_bookshelf_config.view.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_tab_layout.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity


class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    TabLayout.OnTabSelectedListener,
    SearchView.OnQueryTextListener,
    GroupManageDialog.CallBack,
    BookshelfAdapter.CallBack {

    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)

    private lateinit var bookshelfAdapter: BookshelfAdapter
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private val bookGroups = mutableListOf<BookGroup>()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initView()
        initBookGroupData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_search -> startActivity<SearchActivity>()
            R.id.menu_bookshelf_layout -> configBookshelf()
            R.id.menu_group_manage -> GroupManageDialog()
                .show(childFragmentManager, "groupManageDialog")
            R.id.menu_add_local -> startActivity<ImportBookActivity>()
            R.id.menu_add_url -> addBookByUrl()
            R.id.menu_arrange_bookshelf -> startActivity<ArrangeBookActivity>(
                Pair("groupId", selectedGroup.groupId),
                Pair("groupName", selectedGroup.groupName)
            )
            R.id.menu_download -> startActivity<DownloadActivity>()
        }
    }

    override val groupSize: Int
        get() = bookGroups.size

    override fun getGroup(position: Int): BookGroup {
        return bookGroups[position]
    }

    private val selectedGroup: BookGroup
        get() = bookGroups[view_pager_bookshelf.currentItem]

    private fun initView() {
        tab_layout.isTabIndicatorFullWidth = false
        tab_layout.tabMode = TabLayout.MODE_SCROLLABLE
        tab_layout.setSelectedTabIndicatorColor(requireContext().accentColor)
        ATH.applyEdgeEffectColor(view_pager_bookshelf)
        bookshelfAdapter = BookshelfAdapter(this, this)
        view_pager_bookshelf.adapter = bookshelfAdapter
        TabLayoutMediator(tab_layout, view_pager_bookshelf) { tab, position ->
            tab.text = bookGroups[position].groupName
        }.attach()
        observeEvent<Int>(EventBus.UP_TABS) {
            tab_layout.getTabAt(it)?.select()
        }
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = App.db.bookGroupDao().liveDataAll()
        bookGroupLiveData?.observe(viewLifecycleOwner, Observer {
            viewModel.checkGroup(it)
            synchronized(this) {
                tab_layout.removeOnTabSelectedListener(this)
                bookGroups.clear()
                bookGroups.add(AppConst.bookGroupAll)
                if (AppConst.bookGroupLocalShow) {
                    bookGroups.add(AppConst.bookGroupLocal)
                }
                if (AppConst.bookGroupAudioShow) {
                    bookGroups.add(AppConst.bookGroupAudio)
                }
                bookGroups.addAll(it)
                bookshelfAdapter.notifyDataSetChanged()
                tab_layout.getTabAt(getPrefInt(PreferKey.saveTabPosition, 0))?.select()
                tab_layout.addOnTabSelectedListener(this)
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
            bookshelfAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("InflateParams")
    private fun configBookshelf() {
        requireContext().alert(titleResource = R.string.bookshelf_layout) {
            val bookshelfLayout = getPrefInt(PreferKey.bookshelfLayout)
            val bookshelfSort = getPrefInt(PreferKey.bookshelfSort)
            val root = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_bookshelf_config, null).apply {
                    rg_layout.checkByIndex(bookshelfLayout)
                    rg_sort.checkByIndex(bookshelfSort)
                }
            customView = root
            okButton {
                root.apply {
                    var changed = false
                    if (bookshelfLayout != rg_layout.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfLayout, rg_layout.getCheckedIndex())
                        changed = true
                    }
                    if (bookshelfSort != rg_sort.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfSort, rg_sort.getCheckedIndex())
                        changed = true
                    }
                    if (changed) {
                        activity?.recreate()
                    }
                }
            }
            noButton()
        }.show().applyTint()
    }

    @SuppressLint("InflateParams")
    private fun addBookByUrl() {
        requireContext()
            .alert(titleResource = R.string.add_book_url) {
                var editText: AutoCompleteTextView? = null
                customView {
                    layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                        editText = edit_view
                    }
                }
                okButton {
                    editText?.text?.toString()?.let {
                        viewModel.addBookByUrl(it)
                    }
                }
                noButton { }
            }.show().applyTint()
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {

    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {

    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        tab?.position?.let {
            putPrefInt(PreferKey.saveTabPosition, it)
        }
    }
}