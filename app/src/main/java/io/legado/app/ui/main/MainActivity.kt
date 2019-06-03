package io.legado.app.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Restore
import io.legado.app.ui.main.bookshelf.BookshelfFragment
import io.legado.app.ui.main.booksource.BookSourceFragment
import io.legado.app.ui.main.findbook.FindBookFragment
import io.legado.app.ui.main.myconfig.MyConfigFragment
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity<MainViewModel>(), BottomNavigationView.OnNavigationItemSelectedListener,
    ViewPager.OnPageChangeListener {
    private val mFragmentList: ArrayList<Fragment> = ArrayList()

    override val viewModel: MainViewModel
        get() = getViewModel(MainViewModel::class.java)

    override val layoutID: Int
        get() = R.layout.activity_main

    override fun onViewModelCreated(viewModel: MainViewModel, savedInstanceState: Bundle?) {
        mFragmentList.add(BookshelfFragment(R.layout.fragment_bookshelf))
        mFragmentList.add(FindBookFragment(R.layout.fragment_find_book))
        mFragmentList.add(BookSourceFragment(R.layout.fragment_book_source))
        mFragmentList.add(MyConfigFragment(R.layout.fragment_my_config))
        view_pager_main.adapter =
            TabFragmentPageAdapter(supportFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
        view_pager_main.addOnPageChangeListener(this)
        bottom_navigation_view.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_bookshelf -> view_pager_main.currentItem = 0
            R.id.menu_find_book -> view_pager_main.currentItem = 1
            R.id.menu_book_source -> view_pager_main.currentItem = 2
            R.id.menu_my_config -> view_pager_main.currentItem = 3
        }
        return false
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        return super.onCompatOptionsItemSelected(item)
    }

    private fun importYueDu() {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted { Restore.importYueDuData(this) }.request()
    }

    override fun onPageScrollStateChanged(state: Int) {

    }


    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }


    override fun onPageSelected(position: Int) {
        bottom_navigation_view.menu.getItem(position).isChecked = true
    }

    inner class TabFragmentPageAdapter internal constructor(fm: FragmentManager, behavior: Int) :
        FragmentPagerAdapter(fm, behavior) {

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

    }

}
