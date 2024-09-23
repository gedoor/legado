@file:Suppress("DEPRECATION")

package io.legado.app.ui.rss.favorites

import android.os.Bundle
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import io.legado.app.base.BaseActivity
import io.legado.app.data.appDb
import io.legado.app.databinding.ActivityRssFavoritesBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.gone
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.launch

/**
 * 收藏夹
 */
class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>(){

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    private val adapter by lazy { TabFragmentPageAdapter() }
    private var groupList = mutableListOf<String>()
    private val fragmentMap = hashMapOf<String, Fragment>()


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor)
        upFragments()
    }

    private fun upFragments() {
        lifecycleScope.launch {
            appDb.rssStarDao.updateGroup()
            val groups = appDb.rssStarDao.groupList()
            groupList.clear()
            groupList.addAll(groups)
            if (groupList.size == 1) {
                binding.tabLayout.gone()
            } else {
                binding.tabLayout.visible()
            }
            adapter.notifyDataSetChanged()
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
            val sort = groupList[position]
            return RssFavoritesFragment(sort)
        }

        override fun getCount(): Int {
            return groupList.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[groupList[position]] = fragment
            return fragment
        }
    }
}