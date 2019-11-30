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
import io.legado.app.help.storage.Backup
import io.legado.app.lib.theme.ATH
import io.legado.app.service.help.ReadAloud
import io.legado.app.ui.main.bookshelf.BookshelfFragment
import io.legado.app.ui.main.explore.ExploreFragment
import io.legado.app.ui.main.my.MyFragment
import io.legado.app.ui.main.rss.RssFragment
import io.legado.app.ui.widget.page.ChapterProvider
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getViewModel
import io.legado.app.utils.observeEvent
import io.legado.app.utils.putPrefInt
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_book_page.*

class MainActivity : VMBaseActivity<MainViewModel>(R.layout.activity_main),
    BottomNavigationView.OnNavigationItemSelectedListener,
    ViewPager.OnPageChangeListener by ViewPager.SimpleOnPageChangeListener() {

    override val viewModel: MainViewModel
        get() = getViewModel(MainViewModel::class.java)

    private var pagePosition = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        ChapterProvider.textView = content_text_view
        ATH.applyEdgeEffectColor(view_pager_main)
        ATH.applyBottomNavigationColor(bottom_navigation_view)
        view_pager_main.offscreenPageLimit = 3
        view_pager_main.adapter = TabFragmentPageAdapter(supportFragmentManager)
        view_pager_main.addOnPageChangeListener(this)
        bottom_navigation_view.setOnNavigationItemSelectedListener(this)
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

    override fun onDestroy() {
        super.onDestroy()
        ReadAloud.stop(this)
    }

    override fun observeLiveBus() {
        observeEvent<String>(Bus.RECREATE) {
            recreate()
        }
        observeEvent<Boolean>(Bus.UP_CONFIG) {
            content_view.upStyle()
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

