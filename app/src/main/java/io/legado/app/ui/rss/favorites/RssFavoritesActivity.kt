@file:Suppress("DEPRECATION")

package io.legado.app.ui.rss.favorites

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.databinding.ActivityRssFavoritesBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.gone
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 收藏夹
 */
class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>() {

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    private val adapter by lazy { TabFragmentPageAdapter() }
    private var groupList = mutableListOf<String>()
    private var groupsMenu: SubMenu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        upFragments()
    }

    private fun initView() {
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_favorites, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
        return super.onCompatCreateOptionsMenu(menu)
    }

    private fun upGroupsMenu() = groupsMenu?.let { subMenu ->
        subMenu.removeGroup(R.id.menu_group)
        groupList.forEachIndexed { index, it ->
            subMenu.add(R.id.menu_group, Menu.NONE, index, it)
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        if (item.groupId == R.id.menu_group) {
            binding.viewPager.setCurrentItem(item.order)
        } else {
            when (item.itemId) {
                R.id.menu_del_group -> deleteGroup()
                R.id.menu_del_all -> deleteAll()
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun upFragments() {
        lifecycleScope.launch {
            appDb.rssStarDao.flowGroups().catch {
                AppLog.put("订阅分组数据获取失败\n${it.localizedMessage}", it)
            }.distinctUntilChanged().flowOn(IO).collect {
                groupList.clear()
                groupList.addAll(it)
                if (groupList.size == 1) {
                    binding.tabLayout.gone()
                } else {
                    binding.tabLayout.visible()
                }
                if (groupsMenu != null) {
                    upGroupsMenu()
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun deleteGroup() {
        alert(R.string.draw) {
            val item = binding.viewPager.currentItem
            val group = groupList[item]
            setMessage(getString(R.string.sure_del) + "\n<" + group + ">" + getString(R.string.group))
            noButton()
            yesButton {
                appDb.rssStarDao.deleteByGroup(group)
            }
        }
    }

    private fun deleteAll() {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n<" + getString(R.string.all) + ">" + getString(R.string.favorite))
            noButton()
            yesButton {
                appDb.rssStarDao.deleteAll()
            }
        }
    }

    private inner class TabFragmentPageAdapter :
        FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getPageTitle(position: Int): CharSequence {
            return groupList[position]
        }

        override fun getItem(position: Int): Fragment {
            val group = groupList[position]
            return RssFavoritesFragment(group)
        }

        override fun getCount(): Int {
            return groupList.size
        }

    }
}
