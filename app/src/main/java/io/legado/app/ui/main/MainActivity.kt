@file:Suppress("DEPRECATION")

package io.legado.app.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst.appInfo
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ActivityMainBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.LocalConfig
import io.legado.app.help.storage.Backup
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.elevation
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.main.bookshelf.BaseBookshelfFragment
import io.legado.app.ui.main.bookshelf.style1.BookshelfFragment1
import io.legado.app.ui.main.bookshelf.style2.BookshelfFragment2
import io.legado.app.ui.main.explore.ExploreFragment
import io.legado.app.ui.main.my.MyFragment
import io.legado.app.ui.main.rss.RssFragment
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.observeEvent
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding


class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener {

    override val binding by viewBinding(ActivityMainBinding::inflate)
    override val viewModel by viewModels<MainViewModel>()
    private var exitTime: Long = 0
    private var bookshelfReselected: Long = 0
    private var exploreReselected: Long = 0
    private var pagePosition = 0
    private val fragmentMap = hashMapOf<Int, Fragment>()
    private var bottomMenuCount = 2
    private val realPositions = arrayOf(0, 1, 2, 3)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        upBottomMenu()
        binding.apply {
            ATH.applyEdgeEffectColor(viewPagerMain)
            ATH.applyBottomNavigationColor(bottomNavigationView)
            viewPagerMain.offscreenPageLimit = 3
            viewPagerMain.adapter = TabFragmentPageAdapter(supportFragmentManager)
            viewPagerMain.addOnPageChangeListener(PageChangeCallback())
            bottomNavigationView.elevation =
                if (AppConfig.elevation < 0) elevation else AppConfig.elevation.toFloat()
            bottomNavigationView.setOnNavigationItemSelectedListener(this@MainActivity)
            bottomNavigationView.setOnNavigationItemReselectedListener(this@MainActivity)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        upVersion()
        //自动更新书籍
        if (AppConfig.autoRefreshBook) {
            binding.viewPagerMain.postDelayed({
                viewModel.upAllBookToc()
            }, 1000)
        }
        binding.viewPagerMain.postDelayed({
            viewModel.postLoad()
        }, 3000)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean = binding.run {
        when (item.itemId) {
            R.id.menu_bookshelf -> viewPagerMain.setCurrentItem(0, false)
            R.id.menu_discovery -> viewPagerMain.setCurrentItem(1, false)
            R.id.menu_rss -> if (AppConfig.showDiscovery) {
                viewPagerMain.setCurrentItem(2, false)
            } else {
                viewPagerMain.setCurrentItem(1, false)
            }
            R.id.menu_my_config -> viewPagerMain.setCurrentItem(bottomMenuCount - 1, false)
        }
        return false
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_bookshelf -> {
                if (System.currentTimeMillis() - bookshelfReselected > 300) {
                    bookshelfReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[getFragmentId(0)] as? BaseBookshelfFragment)?.gotoTop()
                }
            }
            R.id.menu_discovery -> {
                if (System.currentTimeMillis() - exploreReselected > 300) {
                    exploreReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[1] as? ExploreFragment)?.compressExplore()
                }
            }
        }
    }

    private fun upVersion() {
        if (LocalConfig.versionCode != appInfo.versionCode) {
            LocalConfig.versionCode = appInfo.versionCode
            if (LocalConfig.isFirstOpenApp) {
                val text = String(assets.open("help/appHelp.md").readBytes())
                TextDialog.show(supportFragmentManager, text, TextDialog.MD)
            } else if (!BuildConfig.DEBUG) {
                val log = String(assets.open("updateLog.md").readBytes())
                TextDialog.show(supportFragmentManager, log, TextDialog.MD, 5000, true)
            }
            viewModel.upVersion()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> if (event.isTracking && !event.isCanceled) {
                    if (pagePosition != 0) {
                        binding.viewPagerMain.currentItem = 0
                        return true
                    }
                    if (System.currentTimeMillis() - exitTime > 2000) {
                        toastOnUi(R.string.double_click_exit)
                        exitTime = System.currentTimeMillis()
                    } else {
                        if (BaseReadAloudService.pause) {
                            finish()
                        } else {
                            moveTaskToBack(true)
                        }
                    }
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BookHelp.clearRemovedCache()
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
        observeEvent<Boolean>(EventBus.NOTIFY_MAIN) {
            binding.apply {
                upBottomMenu()
                viewPagerMain.adapter?.notifyDataSetChanged()
                if (it) {
                    viewPagerMain.setCurrentItem(bottomMenuCount - 1, false)
                }
            }
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
    }

    private fun upBottomMenu() {
        val showDiscovery = AppConfig.showDiscovery
        val showRss = AppConfig.showRSS
        binding.bottomNavigationView.menu.let { menu ->
            menu.findItem(R.id.menu_discovery).isVisible = showDiscovery
            menu.findItem(R.id.menu_rss).isVisible = showRss
        }
        bottomMenuCount = 2
        realPositions[1] = 1
        realPositions[2] = 2
        when {
            showDiscovery -> bottomMenuCount++
            showRss -> {
                realPositions[1] = 2
                realPositions[2] = 3
            }
            else -> {
                realPositions[1] = 3
                realPositions[2] = 3
            }
        }
        if (showRss) {
            bottomMenuCount++
        } else {
            realPositions[2] = 3
        }
    }

    private fun getFragmentId(position: Int): Int {
        val p = realPositions[position]
        if (p == 0) {
            return if (AppConfig.bookGroupStyle == 1) 11 else 0
        }
        return p
    }

    private inner class PageChangeCallback : ViewPager.SimpleOnPageChangeListener() {

        override fun onPageSelected(position: Int) {
            pagePosition = position
            binding.bottomNavigationView.menu
                .getItem(realPositions[position]).isChecked = true
        }

    }

    @Suppress("DEPRECATION")
    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private fun getId(position: Int): Int {
            return getFragmentId(position)
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            return when (getId(position)) {
                0 -> BookshelfFragment1()
                11 -> BookshelfFragment2()
                1 -> ExploreFragment()
                2 -> RssFragment()
                else -> MyFragment()
            }
        }

        override fun getCount(): Int {
            return bottomMenuCount
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[getId(position)] = fragment
            return fragment
        }

    }

}