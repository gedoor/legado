package io.legado.app.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.legado.app.App
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Bus
import io.legado.app.help.ActivityHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.main.bookshelf.BookshelfFragment
import io.legado.app.ui.main.explore.ExploreFragment
import io.legado.app.ui.main.my.MyFragment
import io.legado.app.ui.main.rss.RssFragment
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : VMBaseActivity<MainViewModel>(R.layout.activity_main),
    BottomNavigationView.OnNavigationItemSelectedListener,
    ViewPager.OnPageChangeListener by ViewPager.SimpleOnPageChangeListener() {

    override val viewModel: MainViewModel
        get() = getViewModel(MainViewModel::class.java)

    private var pagePosition = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        ATH.applyEdgeEffectColor(view_pager_main)
        ATH.applyBottomNavigationColor(bottom_navigation_view)
        view_pager_main.offscreenPageLimit = 3
        view_pager_main.adapter = TabFragmentPageAdapter(supportFragmentManager)
        view_pager_main.addOnPageChangeListener(this)
        bottom_navigation_view.setOnNavigationItemSelectedListener(this)
        restore()
        upVersion()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_bookshelf -> view_pager_main.setCurrentItem(0, false)
            R.id.menu_find_book -> view_pager_main.setCurrentItem(1, false)
            R.id.menu_rss -> view_pager_main.setCurrentItem(2, false)
            R.id.menu_my_config -> view_pager_main.setCurrentItem(3, false)
        }
        return false
    }

    private fun restore() {
        if (getPrefBoolean("dbChange")) {
            Restore.restore()
        } else if (getPrefInt("versionCode") == 0) {
            launch {
                if (withContext(IO) { App.db.bookDao().allBookCount == 0 }) {
                    alert(title = "导入") {
                        message = "是否导入旧版本数据"
                        yesButton {
                            PermissionsCompat.Builder(this@MainActivity)
                                .addPermissions(*Permissions.Group.STORAGE)
                                .rationale(R.string.tip_perm_request_storage)
                                .onGranted { viewModel.importYueDuData() }
                                .request()
                        }
                        noButton {
                        }
                    }.show().applyTint()
                }
            }
        }
    }

    private fun upVersion() {
        if (getPrefInt("versionCode") != App.INSTANCE.versionCode) {
            putPrefInt("versionCode", App.INSTANCE.versionCode)
        }
    }

    override fun onPageSelected(position: Int) {
        pagePosition = position
        bottom_navigation_view.menu.getItem(position).isChecked = true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> if (
                    pagePosition != 0
                    && event.isTracking
                    && !event.isCanceled
                ) {
                    view_pager_main.currentItem = 0
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun finish() {
        if (ActivityHelp.size() > 1) {
            moveTaskToBack(true)
        } else {
            if (!BuildConfig.DEBUG) {
                Backup.autoBackup()
            }
            super.finish()
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(Bus.RECREATE) {
            recreate()
        }
    }

    private class TabFragmentPageAdapter internal constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> BookshelfFragment()
                1 -> ExploreFragment()
                2 -> RssFragment()
                else -> MyFragment()
            }
        }

        override fun getCount(): Int {
            return 4
        }
    }
}

