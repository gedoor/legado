@file:Suppress("DEPRECATION")

package io.legado.app.ui.main.bookshelf.style1

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.FragmentBookshelfBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.group.GroupEditDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.main.bookshelf.BaseBookshelfFragment
import io.legado.app.ui.main.bookshelf.style1.books.BooksFragment
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.collections.set

/**
 * 书架界面
 */
class BookshelfFragment1 : BaseBookshelfFragment(R.layout.fragment_bookshelf),
    TabLayout.OnTabSelectedListener,
    SearchView.OnQueryTextListener {

    private val binding by viewBinding(FragmentBookshelfBinding::bind)
    private val adapter by lazy { TabFragmentPageAdapter(childFragmentManager) }
    private val tabLayout: TabLayout by lazy {
        binding.titleBar.findViewById(R.id.tab_layout)
    }
    private val bookGroups = mutableListOf<BookGroup>()
    private val fragmentMap = hashMapOf<Long, BooksFragment>()
    override val groupId: Long get() = selectedGroup?.groupId ?: 0

    override val books: List<Book>
        get() {
            val fragment = fragmentMap[groupId]
            return fragment?.getBooks() ?: emptyList()
        }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initView()
        initBookGroupData()
    }

    private val selectedGroup: BookGroup?
        get() = bookGroups.getOrNull(tabLayout.selectedTabPosition)

    private fun initView() {
        binding.viewPagerBookshelf.setEdgeEffectColor(primaryColor)
        tabLayout.isTabIndicatorFullWidth = false
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setSelectedTabIndicatorColor(requireContext().accentColor)
        tabLayout.setupWithViewPager(binding.viewPagerBookshelf)
        binding.viewPagerBookshelf.offscreenPageLimit = 1
        binding.viewPagerBookshelf.adapter = adapter
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        SearchActivity.start(requireContext(), query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    @Synchronized
    override fun upGroup(data: List<BookGroup>) {
        if (data.isEmpty()) {
            appDb.bookGroupDao.enableGroup(AppConst.bookGroupAllId)
        } else {
            if (data != bookGroups) {
                bookGroups.clear()
                bookGroups.addAll(data)
                adapter.notifyDataSetChanged()
                selectLastTab()
                for (i in 0 until adapter.count) {
                    tabLayout.getTabAt(i)?.view?.setOnLongClickListener {
                        showDialogFragment(GroupEditDialog(bookGroups[i]))
                        true
                    }
                }
            }
        }
    }

    @Synchronized
    private fun selectLastTab() {
        tabLayout.removeOnTabSelectedListener(this)
        tabLayout.getTabAt(getPrefInt(PreferKey.saveTabPosition, 0))?.select()
        tabLayout.addOnTabSelectedListener(this)
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        selectedGroup?.let { group ->
            fragmentMap[group.groupId]?.let {
                toastOnUi("${group.groupName}(${it.getBooksCount()})")
            }
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabSelected(tab: TabLayout.Tab) {
        putPrefInt(PreferKey.saveTabPosition, tab.position)
    }

    override fun gotoTop() {
        fragmentMap[groupId]?.gotoTop()
    }

    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getPageTitle(position: Int): CharSequence {
            return bookGroups[position].groupName
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            val group = bookGroups[position]
            return BooksFragment(position, group)
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