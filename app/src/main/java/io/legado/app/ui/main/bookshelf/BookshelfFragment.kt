package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import io.legado.app.databinding.DialogBookshelfConfigBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.FragmentBookshelfBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.arrange.ArrangeBookActivity
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.local.ImportBookActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.filepicker.FilePicker
import io.legado.app.ui.filepicker.FilePickerDialog
import io.legado.app.ui.main.MainViewModel
import io.legado.app.ui.main.bookshelf.books.BooksFragment
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import org.jetbrains.anko.share

/**
 * 书架界面
 */
class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    TabLayout.OnTabSelectedListener,
    FilePickerDialog.CallBack,
    SearchView.OnQueryTextListener {

    private val requestCodeImportBookshelf = 312
    private val binding by viewBinding(FragmentBookshelfBinding::bind)
    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)
    private val activityViewModel: MainViewModel
        get() = getViewModelOfActivity(MainViewModel::class.java)
    private lateinit var adapter: FragmentStatePagerAdapter
    private lateinit var tabLayout: TabLayout
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private val bookGroups = mutableListOf<BookGroup>()
    private val fragmentMap = hashMapOf<Long, BooksFragment>()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        tabLayout = binding.titleBar.findViewById(R.id.tab_layout)
        setSupportToolbar(binding.titleBar.toolbar)
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
                val fragment = fragmentMap[selectedGroup.groupId]
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
                Pair("groupId", selectedGroup.groupId ?: 0),
                Pair("groupName", selectedGroup.groupName ?: 0)
            )
            R.id.menu_download -> startActivity<CacheActivity>(
                Pair("groupId", selectedGroup.groupId ?: 0),
                Pair("groupName", selectedGroup.groupName ?: 0)
            )
            R.id.menu_export_bookshelf -> {
                val fragment = fragmentMap[selectedGroup.groupId]
                viewModel.exportBookshelf(fragment?.getBooks()) {
                    activity?.share(it)
                }
            }
            R.id.menu_import_bookshelf -> importBookshelfAlert()
        }
    }

    private val selectedGroup: BookGroup
        get() = bookGroups[tabLayout.selectedTabPosition]

    private fun initView() {
        ATH.applyEdgeEffectColor(binding.viewPagerBookshelf)
        tabLayout.isTabIndicatorFullWidth = false
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setSelectedTabIndicatorColor(requireContext().accentColor)
        tabLayout.setupWithViewPager(binding.viewPagerBookshelf)
        binding.viewPagerBookshelf.offscreenPageLimit = 1
        adapter = TabFragmentPageAdapter(childFragmentManager)
        binding.viewPagerBookshelf.adapter = adapter
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = App.db.bookGroupDao.liveDataShow().apply {
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
            App.db.bookGroupDao.enableGroup(AppConst.bookGroupAllId)
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
        tabLayout.removeOnTabSelectedListener(this)
        tabLayout.getTabAt(getPrefInt(PreferKey.saveTabPosition, 0))?.select()
        tabLayout.addOnTabSelectedListener(this)
    }

    @SuppressLint("InflateParams")
    private fun configBookshelf() {
        alert(titleResource = R.string.bookshelf_layout) {
            val bookshelfLayout = getPrefInt(PreferKey.bookshelfLayout)
            val bookshelfSort = getPrefInt(PreferKey.bookshelfSort)
            val alertBinding =
                DialogBookshelfConfigBinding.inflate(layoutInflater)
                    .apply {
                        rgLayout.checkByIndex(bookshelfLayout)
                        rgSort.checkByIndex(bookshelfSort)
                    }
            customView = alertBinding.root
            okButton {
                alertBinding.apply {
                    var changed = false
                    if (bookshelfLayout != rgLayout.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfLayout, rgLayout.getCheckedIndex())
                        changed = true
                    }
                    if (bookshelfSort != rgSort.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfSort, rgSort.getCheckedIndex())
                        changed = true
                    }
                    if (changed) {
                        activity?.recreate()
                    }
                }
            }
            noButton()
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun addBookByUrl() {
        alert(titleResource = R.string.add_book_url) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            customView = alertBinding.root
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.addBookByUrl(it)
                }
            }
            noButton()
        }.show()
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        fragmentMap[selectedGroup.groupId]?.let {
            toast("${selectedGroup.groupName}(${it.getBooksCount()})")
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabSelected(tab: TabLayout.Tab) {
        putPrefInt(PreferKey.saveTabPosition, tab.position)
    }

    fun gotoTop() {
        fragmentMap[selectedGroup.groupId]?.gotoTop()
    }

    private fun importBookshelfAlert() {
        alert(titleResource = R.string.import_bookshelf) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url/json"
            }
            customView = alertBinding.root
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.importBookshelf(it, selectedGroup.groupId)
                }
            }
            noButton()
            neutralButton(R.string.select_file) {
                FilePicker.selectFile(
                    this@BookshelfFragment,
                    requestCodeImportBookshelf,
                    allowExtensions = arrayOf("txt", "json")
                )
            }
        }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            requestCodeImportBookshelf -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    uri.readText(requireContext())?.let {
                        viewModel.importBookshelf(it, selectedGroup.groupId)
                    }
                }
            }
        }
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
            return BooksFragment.newInstance(position, group.groupId)
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