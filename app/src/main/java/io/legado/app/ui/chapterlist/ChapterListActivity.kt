package io.legado.app.ui.chapterlist

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_chapter_list.*
import kotlinx.android.synthetic.main.view_title_bar.*

class ChapterListActivity : BaseActivity(R.layout.activity_chapter_list) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        tab_layout.visible()
        view_pager.adapter = TabFragmentPageAdapter(supportFragmentManager)
        tab_layout.setupWithViewPager(view_pager)
    }


    private inner class TabFragmentPageAdapter internal constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                1 -> BookmarkFragment()
                else -> ChapterListFragment()
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                1 -> getString(R.string.bookmark)
                else -> getString(R.string.chapter_list)
            }
        }

    }
}