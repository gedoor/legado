package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.LiveData
import com.google.android.material.tabs.TabLayout
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.BookGroup
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.arrange.ArrangeBookActivity
import io.legado.app.ui.book.download.DownloadActivity
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.local.ImportBookActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.main.MainViewModel
import io.legado.app.ui.main.bookshelf.books.BooksFragment
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_bookshelf_config.view.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_tab_layout.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.startActivity


class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    TabLayout.OnTabSelectedListener,
    SearchView.OnQueryTextListener,
    GroupManageDialog.CallBack {

    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)
    private val activityViewModel: MainViewModel
        get() = getViewModelOfActivity(MainViewModel::class.java)
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private var noGroupLiveData: LiveData<Int>? = null
    private val bookGroups = mutableListOf<BookGroup>()
    private val fragmentMap = hashMapOf<Int, BooksFragment>()
    private var showGroupNone = false

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
            R.id.menu_update_toc -> {
                val group = bookGroups[tab_layout.selectedTabPosition]
                val fragment = fragmentMap[group.groupId]
                fragment?.getBooks()?.let {
                    activityViewModel.upToc(it)
                }
            }
            R.id.menu_bookshelf_layout -> configBookshelf()
            R.id.menu_group_manage -> GroupManageDialog()
                .show(childFragmentManager, "groupManageDialog")
            R.id.menu_add_local -> startActivity<ImportBookActivity>()
            R.id.menu_add_url -> addBookByUrl()
            R.id.menu_arrange_bookshelf -> startActivity<ArrangeBookActivity>(
                Pair("groupId", selectedGroup?.groupId ?: 0),
                Pair("groupName", selectedGroup?.groupName ?: 0)
            )
            R.id.menu_download -> startActivity<DownloadActivity>(
                Pair("groupId", selectedGroup?.groupId ?: 0),
                Pair("groupName", selectedGroup?.groupName ?: 0)
            )
        }
    }

    private val selectedGroup: BookGroup?
        get() = bookGroups.getOrNull(view_pager_bookshelf?.currentItem ?: 0)

    private fun initView() {
        ATH.applyEdgeEffectColor(view_pager_bookshelf)
        tab_layout.isTabIndicatorFullWidth = false
        tab_layout.tabMode = TabLayout.MODE_SCROLLABLE
        tab_layout.setSelectedTabIndicatorColor(requireContext().accentColor)
        tab_layout.setupWithViewPager(view_pager_bookshelf)
        view_pager_bookshelf.offscreenPageLimit = 1
        view_pager_bookshelf.adapter = TabFragmentPageAdapter(childFragmentManager)
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = App.db.bookGroupDao().liveDataAll()
        bookGroupLiveData?.observe(viewLifecycleOwner, {
            viewModel.checkGroup(it)
            launch {
                synchronized(this) {
                    tab_layout.removeOnTabSelectedListener(this@BookshelfFragment)
                }
                var noGroupSize = 0
                withContext(IO) {
                    if (AppConfig.bookGroupNoneShow) {
                        noGroupSize = App.db.bookDao().noGroupSize
                    }
                }
                synchronized(this@BookshelfFragment) {
                    bookGroups.clear()
                    if (AppConfig.bookGroupAllShow) {
                        bookGroups.add(AppConst.bookGroupAll)
                    }
                    if (AppConfig.bookGroupLocalShow) {
                        bookGroups.add(AppConst.bookGroupLocal)
                    }
                    if (AppConfig.bookGroupAudioShow) {
                        bookGroups.add(AppConst.bookGroupAudio)
                    }
                    showGroupNone = if (noGroupSize > 0 && it.isNotEmpty()) {
                        bookGroups.add(AppConst.bookGroupNone)
                        true
                    } else {
                        false
                    }
                    bookGroups.addAll(it)
                    view_pager_bookshelf.adapter?.notifyDataSetChanged()
                    tab_layout.getTabAt(getPrefInt(PreferKey.saveTabPosition, 0))?.select()
                    tab_layout.addOnTabSelectedListener(this@BookshelfFragment)
                }
            }
        })
        noGroupLiveData?.removeObservers(viewLifecycleOwner)
        noGroupLiveData = App.db.bookDao().observeNoGroupSize()
        noGroupLiveData?.observe(viewLifecycleOwner, {
            if (it > 0 && !showGroupNone && AppConfig.bookGroupNoneShow) {
                showGroupNone = true
                upGroup()
            } else if (it == 0 && showGroupNone) {
                showGroupNone = false
                upGroup()
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
        launch {
            var noGroupSize = 0
            withContext(IO) {
                if (AppConfig.bookGroupNoneShow) {
                    noGroupSize = App.db.bookDao().noGroupSize
                }
            }
            synchronized(this@BookshelfFragment) {
                bookGroups.remove(AppConst.bookGroupAll)
                bookGroups.remove(AppConst.bookGroupLocal)
                bookGroups.remove(AppConst.bookGroupAudio)
                bookGroups.remove(AppConst.bookGroupNone)
                showGroupNone =
                    if (noGroupSize > 0 && bookGroups.isNotEmpty()) {
                        bookGroups.add(0, AppConst.bookGroupNone)
                        true
                    } else {
                        false
                    }
                if (AppConfig.bookGroupAudioShow) {
                    bookGroups.add(0, AppConst.bookGroupAudio)
                }
                if (AppConfig.bookGroupLocalShow) {
                    bookGroups.add(0, AppConst.bookGroupLocal)
                }
                if (AppConfig.bookGroupAllShow) {
                    bookGroups.add(0, AppConst.bookGroupAll)
                }
                view_pager_bookshelf.adapter?.notifyDataSetChanged()
            }
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

    override fun onTabReselected(tab: TabLayout.Tab?) = Unit

    override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

    override fun onTabSelected(tab: TabLayout.Tab?) {
        tab?.position?.let {
            putPrefInt(PreferKey.saveTabPosition, it)
        }
    }

    fun gotoTop() {
        fragmentMap[selectedGroup?.groupId]?.gotoTop()
    }

    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getPageTitle(position: Int): CharSequence? {
            return bookGroups[position].groupName
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            val group = bookGroups[position]
            var fragment = fragmentMap[group.groupId]
            if (fragment == null) {
                fragment = BooksFragment.newInstance(position, group.groupId)
                fragmentMap[group.groupId] = fragment
            }
            return fragment
        }

        override fun getCount(): Int {
            return bookGroups.size
        }

    }
}