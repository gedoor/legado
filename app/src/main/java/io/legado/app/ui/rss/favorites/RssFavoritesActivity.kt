package io.legado.app.ui.rss.favorites

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 收藏夹
 */
class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>() {

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    private val adapter by lazy { TabFragmentPageAdapter(this) }
    private var groupList = mutableListOf<String>()
    private var groupsMenu: SubMenu? = null
    private var currentGroup = ""

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        upFragments()
    }

    override fun onResume() {
        super.onResume()
        //从ReadRssActivity退出时，判断是否需要重新定位tabLayout选中项
        if (currentGroup.isNotEmpty() && groupList.isNotEmpty()){
            var item = groupList.indexOf(currentGroup)
            val currentItem = binding.viewPager.currentItem
            //如果坐标没有变化，则结束
            if(item == currentItem){
                return
            }
            if (item == -1){
                item = currentItem
            }
            lifecycleScope.launch {
                delay(100)
                binding.tabLayout.getTabAt(item)?.select()
            }
        }
    }

    private fun initView() {
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentGroup = groupList[position]
            }
        })
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = groupList[position]
        }.attach()
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
            binding.viewPager.currentItem = item.order
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
                val callBack = PageDiffUtil(groupList, it)
                val diff = DiffUtil.calculateDiff(callBack)
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
                diff.dispatchUpdatesTo(adapter)
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

    private inner class TabFragmentPageAdapter(rssFavoritesActivity: RssFavoritesActivity) :
        FragmentStateAdapter(rssFavoritesActivity) {
        override fun getItemCount(): Int {
            return groupList.size
        }

        override fun createFragment(position: Int): Fragment {
            val group = groupList[position]
            return RssFavoritesFragment(group)
        }

        override fun getItemId(position: Int): Long {
            return groupList[position].hashCode().toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return groupList.any { it.hashCode().toLong() == itemId }
        }
    }

    //DiffUtil对比差异
    private inner class PageDiffUtil(val oldList: List<String>, val newList: List<String>): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].hashCode() == newList[newItemPosition].hashCode()
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].hashCode() == newList[newItemPosition].hashCode()
        }

    }
}
