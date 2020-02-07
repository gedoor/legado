package io.legado.app.ui.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.legado.app.help.AppConfig
import io.legado.app.ui.main.bookshelf.BookshelfFragment
import io.legado.app.ui.main.explore.ExploreFragment
import io.legado.app.ui.main.my.MyFragment
import io.legado.app.ui.main.rss.RssFragment

class MainAdapter(val activity: MainActivity) : FragmentStateAdapter(activity) {

    private val fid1 = 111.toLong()
    private val fid2 = 222.toLong()
    private val fid3 = 333.toLong()
    private val fid4 = 444.toLong()

    private val ids: ArrayList<Long>
        get() = if (AppConfig.isShowRSS) {
            arrayListOf(fid1, fid2, fid3, fid4)
        } else {
            arrayListOf(fid1, fid2, fid4)
        }

    private val createdIds = hashSetOf<Long>()

    override fun getItemCount(): Int {
        return if (AppConfig.isShowRSS) 4 else 3
    }

    override fun getItemId(position: Int): Long {
        return ids[position]
    }

    override fun containsItem(itemId: Long): Boolean {
        return createdIds.contains(itemId)
    }

    override fun createFragment(position: Int): Fragment {
        val id = ids[position]
        createdIds.add(id)
        return when (id) {
            fid4 -> MyFragment()
            fid3 -> RssFragment()
            fid2 -> ExploreFragment()
            else -> BookshelfFragment()
        }
    }
}