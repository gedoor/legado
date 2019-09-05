package io.legado.app.ui.main

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Bus
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.main.bookshelf.BookshelfFragment
import io.legado.app.ui.main.explore.FindBookFragment
import io.legado.app.ui.main.my.MyFragment
import io.legado.app.ui.main.rss.RssFragment
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getViewModel
import io.legado.app.utils.observeEvent
import io.legado.app.utils.putPrefInt
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : VMBaseActivity<MainViewModel>(R.layout.activity_main),
    BottomNavigationView.OnNavigationItemSelectedListener,
    ViewPager.OnPageChangeListener by ViewPager.SimpleOnPageChangeListener() {

    override val viewModel: MainViewModel
        get() = getViewModel(MainViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        ATH.applyEdgeEffectColor(view_pager_main)
        ATH.applyBottomNavigationColor(bottom_navigation_view)
        view_pager_main.offscreenPageLimit = 3
        view_pager_main.adapter = TabFragmentPageAdapter(supportFragmentManager)
        view_pager_main.addOnPageChangeListener(this)
        bottom_navigation_view.setOnNavigationItemSelectedListener(this)
        importYueDu()
        upVersion()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_bookshelf -> view_pager_main.currentItem = 0
            R.id.menu_find_book -> view_pager_main.currentItem = 1
            R.id.menu_rss -> view_pager_main.currentItem = 2
            R.id.menu_my_config -> view_pager_main.currentItem = 3
        }
        return false
    }

    private fun importYueDu() {
        launch {
            if (withContext(IO) { App.db.bookDao().allBookCount == 0 }) {
                PermissionsCompat.Builder(this@MainActivity)
                    .addPermissions(*Permissions.Group.STORAGE)
                    .rationale(R.string.tip_perm_request_storage)
                    .onGranted { viewModel.restore() }
                    .request()
            }
        }
    }

    private fun upVersion() {
        if (getPrefInt("versionCode") != App.INSTANCE.versionCode) {
            putPrefInt("versionCode", App.INSTANCE.versionCode)
        }
    }

    override fun onPageSelected(position: Int) {
        bottom_navigation_view.menu.getItem(position).isChecked = true
    }

    private inner class TabFragmentPageAdapter internal constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> BookshelfFragment()
                1 -> FindBookFragment()
                2 -> RssFragment()
                else -> MyFragment()
            }
        }

        override fun getCount(): Int {
            return 4
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(Bus.RECREATE) {
            recreate()
        }
    }
}
