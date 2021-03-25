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
import io.legado.app.help.DefaultData
import io.legado.app.help.LocalConfig
import io.legado.app.help.storage.Backup
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.elevation
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.main.bookshelf.BookshelfFragment
import io.legado.app.ui.main.explore.ExploreFragment
import io.legado.app.ui.main.my.MyFragment
import io.legado.app.ui.main.rss.RssFragment
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.observeEvent
import io.legado.app.utils.toastOnUi


class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>(),
    BottomNavigationView.OnNavigationItemSelectedListener,
    BottomNavigationView.OnNavigationItemReselectedListener,
    ViewPager.OnPageChangeListener by ViewPager.SimpleOnPageChangeListener() {
    override val viewModel: MainViewModel by viewModels()
    private var exitTime: Long = 0
    private var bookshelfReselected: Long = 0
    private var exploreReselected: Long = 0
    private var pagePosition = 0
    private val fragmentMap = hashMapOf<Int, Fragment>()

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) = with(binding) {
        ATH.applyEdgeEffectColor(viewPagerMain)
        ATH.applyBottomNavigationColor(bottomNavigationView)
        viewPagerMain.offscreenPageLimit = 3
        viewPagerMain.adapter = TabFragmentPageAdapter(supportFragmentManager)
        viewPagerMain.addOnPageChangeListener(this@MainActivity)
        bottomNavigationView.elevation =
            if (AppConfig.elevation < 0) elevation else AppConfig.elevation.toFloat()
        bottomNavigationView.setOnNavigationItemSelectedListener(this@MainActivity)
        bottomNavigationView.setOnNavigationItemReselectedListener(this@MainActivity)
        bottomNavigationView.menu.findItem(R.id.menu_rss).isVisible = AppConfig.isShowRSS
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean = with(binding) {
        when (item.itemId) {
            R.id.menu_bookshelf -> viewPagerMain.setCurrentItem(0, false)
            R.id.menu_explore -> viewPagerMain.setCurrentItem(1, false)
            R.id.menu_rss -> viewPagerMain.setCurrentItem(2, false)
            R.id.menu_my_config -> viewPagerMain.setCurrentItem(3, false)
        }
        return false
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_bookshelf -> {
                if (System.currentTimeMillis() - bookshelfReselected > 300) {
                    bookshelfReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[0] as? BookshelfFragment)?.gotoTop()
                }
            }
            R.id.menu_explore -> {
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
                DefaultData.importDefaultTocRules()//版本更新时更新自带本地txt目录规则
            }
            viewModel.upVersion()
        }
    }

    override fun onPageSelected(position: Int) = with(binding) {
        pagePosition = position
        when (position) {
            0, 1, 3 -> bottomNavigationView.menu.getItem(position).isChecked = true
            2 -> if (AppConfig.isShowRSS) {
                bottomNavigationView.menu.getItem(position).isChecked = true
            } else {
                bottomNavigationView.menu.getItem(3).isChecked = true
            }
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
        observeEvent<String>(EventBus.SHOW_RSS) {
            binding.bottomNavigationView.menu.findItem(R.id.menu_rss).isVisible =
                AppConfig.isShowRSS
            binding.viewPagerMain.adapter?.notifyDataSetChanged()
            if (AppConfig.isShowRSS) {
                binding.viewPagerMain.setCurrentItem(3, false)
            }
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
    }

    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private fun getId(position: Int): Int {
            return when (position) {
                2 -> if (AppConfig.isShowRSS) 2 else 3
                else -> position
            }
        }

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            return when (getId(position)) {
                0 -> BookshelfFragment()
                1 -> ExploreFragment()
                2 -> RssFragment()
                else -> MyFragment()
            }
        }

        override fun getCount(): Int {
            return if (AppConfig.isShowRSS) 4 else 3
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[getId(position)] = fragment
            return fragment
        }

    }

}