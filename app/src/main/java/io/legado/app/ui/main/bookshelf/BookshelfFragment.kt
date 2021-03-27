package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.*
import androidx.lifecycle.LiveData
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
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
import io.legado.app.ui.main.MainActivity
import io.legado.app.ui.main.MainViewModel
import io.legado.app.ui.main.bookshelf.books.BooksFragment
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 书架界面
 */
class BookshelfFragment : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf),
    TabLayout.OnTabSelectedListener,
    FilePickerDialog.CallBack,
    SearchView.OnQueryTextListener {

    private val requestCodeImportBookshelf = 312
    private val binding by viewBinding(FragmentBookshelfBinding::bind)
    override val viewModel: BookshelfViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private lateinit var tabLayout: TabLayout
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private val bookGroups = mutableListOf<BookGroup>()
    private val fragmentMap = hashMapOf<Long, BooksFragment>()

    private var currentPosition = 0     //当前滑动位置
    private var oldPosition = 0          //上一个滑动位置
    private var currentState = 0        //记录当前手指按下状态
    private var scrolledPixList = mutableListOf<Int>() //记录手指滑动时的像素坐标记录

    private val mainActivity get() = activity as MainActivity

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
            R.id.menu_arrange_bookshelf -> startActivity<ArrangeBookActivity> {
                putExtra("groupId", selectedGroup.groupId)
                putExtra("groupName", selectedGroup.groupName)
            }
            R.id.menu_download -> startActivity<CacheActivity> {
                putExtra("groupId", selectedGroup.groupId)
                putExtra("groupName", selectedGroup.groupName)
            }
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

    private fun initView() = with(binding) {
        ATH.applyEdgeEffectColor(viewPagerBookshelf)
        tabLayout.isTabIndicatorFullWidth = false
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.setSelectedTabIndicatorColor(requireContext().accentColor)
        viewPagerBookshelf.offscreenPageLimit = 1
        viewPagerBookshelf.adapter = TabFragmentPageAdapter()
        TabLayoutMediator(tabLayout, viewPagerBookshelf) { tab, i ->
            tab.text = bookGroups[i].groupName
        }.attach()
        viewPagerBookshelf.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            //页面滚动的位置信息回调： position 当前滚动到哪个页面，positionOffset 位置偏移百分比, positionOffsetPixels 当前所在页面偏移量
            //此回调会触发完onPageScrollStateChanged 的 state 值为1时后面才触发回调
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                currentPosition = position
                if (currentState == 1) {
                    //手指按下滑动坐标记录
                    scrolledPixList.add(positionOffsetPixels)
                }
            }


            // 滚动状态改变回调，state的值分别有0，1，2 ;
            // 0为ViewPager所有事件(1,2)已结束触发
            // 1为在viewPager里按下并滑动触发多次
            // 2是手指抬起触发
            override fun onPageScrollStateChanged(state: Int) {
                currentState = state
                if (state == 0) {
                    if (currentPosition == oldPosition) {
                        when (currentPosition) {
                            0 -> {
                                if (scrolledPixList.size > 1 && scrolledPixList.last() == 0 || scrolledPixList.last() - scrolledPixList[0] > 0) {
                                    //有可能出现滑到一半放弃的情况也是可以出现currentPosition == oldPositon=0，则先判断是否是往右滑时放弃
                                    return
                                }
                                //若还有上一个bottom fragment页面则切换
                                mainActivity.viewPager.currentItem.takeIf { it > 0 }
                                    ?.also { mainActivity.viewPager.setCurrentItem(it - 1, true) }
                            }

                            (viewPagerBookshelf.adapter as FragmentStateAdapter).itemCount - 1 -> {
                                //若还有下一个bottom fragment页面则切换
                                mainActivity.viewPager.currentItem.takeIf { it < mainActivity.viewPager.adapter!!.itemCount - 1 }
                                    ?.also { mainActivity.viewPager.setCurrentItem(it + 1, true) }
                            }
                        }
                    }
                    oldPosition = currentPosition
                    scrolledPixList.clear()//清空滑动记录
                }
            }

        })
    }

    private fun initBookGroupData() {
        bookGroupLiveData?.removeObservers(viewLifecycleOwner)
        bookGroupLiveData = appDb.bookGroupDao.liveDataShow().apply {
            observe(viewLifecycleOwner) {
                viewModel.checkGroup(it)
                upGroup(it)
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        startActivity<SearchActivity> {
            putExtra("key", query)
        }
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    @Synchronized
    private fun upGroup(data: List<BookGroup>) {
        if (data.isEmpty()) {
            appDb.bookGroupDao.enableGroup(AppConst.bookGroupAllId)
        } else {
            if (data != bookGroups) {
                bookGroups.clear()
                bookGroups.addAll(data)
                binding.viewPagerBookshelf.adapter?.notifyDataSetChanged()
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
            customView { alertBinding.root }
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
            customView { alertBinding.root }
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
            toastOnUi("${selectedGroup.groupName}(${it.getBooksCount()})")
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
            customView { alertBinding.root }
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

    private inner class TabFragmentPageAdapter :
        FragmentStateAdapter(this) {

        override fun getItemId(position: Int): Long {
            val group = bookGroups[position]
            return group.groupId
        }

        override fun getItemCount(): Int {
            return bookGroups.size
        }

        override fun createFragment(position: Int): Fragment {
            val group = bookGroups[position]
            val fragment = BooksFragment.newInstance(position, group.groupId)
            fragmentMap[group.groupId] = fragment
            return fragment
        }

    }
}