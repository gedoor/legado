package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
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
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.arrange.ArrangeBookActivity
import io.legado.app.ui.book.cache.CacheActivity
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

/**
 * 书架界面
 */
class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    TabLayout.OnTabSelectedListener,
    SearchView.OnQueryTextListener {

    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)
    private val activityViewModel: MainViewModel
        get() = getViewModelOfActivity(MainViewModel::class.java)
    private lateinit var adapter: FragmentStatePagerAdapter
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private val bookGroups = mutableListOf<BookGroup>()
    private val fragmentMap = hashMapOf<Long, BooksFragment>()

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
            R.id.menu_download -> startActivity<CacheActivity>(
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
        adapter = TabFragmentPageAdapter(childFragmentManager)
        view_pager_bookshelf.adapter = adapter
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = App.db.bookGroupDao().liveDataShow().apply {
            observe(viewLifecycleOwner) {
                viewModel.checkGroup(it)
                upGroup(it)
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        startActivity<SearchActivity>(Pair("key", query))
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    @Synchronized
    private fun upGroup(data: List<BookGroup>) {
        if (data.isEmpty()) {
            App.db.bookGroupDao().enableGroup(AppConst.bookGroupAllId)
        } else {
            if (data != bookGroups) {
                bookGroups.clear()
                bookGroups.addAll(data)
                adapter.notifyDataSetChanged()
                selectLastTab()
            }
        }
    }

    @Synchronized
    private fun selectLastTab() {
        tab_layout.removeOnTabSelectedListener(this)
        tab_layout.getTabAt(getPrefInt(PreferKey.saveTabPosition, 0))?.select()
        tab_layout.addOnTabSelectedListener(this)
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
        alert(titleResource = R.string.add_book_url) {
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

    override fun onTabReselected(tab: TabLayout.Tab) {
        fragmentMap[selectedGroup?.groupId]?.let {
            toast("${selectedGroup?.groupName}(${it.getBooksCount()})")
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabSelected(tab: TabLayout.Tab) {
        putPrefInt(PreferKey.saveTabPosition, tab.position)
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

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as BooksFragment
            val group = bookGroups[position]
            fragmentMap[group.groupId] = fragment
            return fragment
        }

    }
}