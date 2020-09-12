package io.legado.app.ui.book.searchContent

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_chapter_list.*
import kotlinx.android.synthetic.main.view_tab_layout.*


class SearchListActivity : VMBaseActivity<SearchListViewModel>(R.layout.activity_search_list) {
    override val viewModel: SearchListViewModel
        get() = getViewModel(SearchListViewModel::class.java)

    private var searchView: SearchView? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        tab_layout.isTabIndicatorFullWidth = false
        tab_layout.setSelectedTabIndicatorColor(accentColor)
        intent.getStringExtra("bookUrl")?.let {
            viewModel.initBook(it) {
                view_pager.adapter = TabFragmentPageAdapter(supportFragmentManager)
                tab_layout.setupWithViewPager(view_pager)
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_view, menu)
        val search = menu.findItem(R.id.menu_search)
        searchView = search.actionView as SearchView
        ATH.setTint(searchView!!, primaryTextColor)
        searchView?.maxWidth = resources.displayMetrics.widthPixels
        searchView?.onActionViewCollapsed()
        searchView?.setOnCloseListener {
            tab_layout.visible()
            false
        }
        searchView?.setOnSearchClickListener { tab_layout.gone() }
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.startContentSearch(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                return false
            }
        })
        return super.onCompatCreateOptionsMenu(menu)
    }

    private inner class TabFragmentPageAdapter internal constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return SearchListFragment()
        }

        override fun getCount(): Int {
            return 1
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return "Search"
        }

    }

    override fun onBackPressed() {
        if (tab_layout.isGone) {
            searchView?.onActionViewCollapsed()
            tab_layout.visible()
        } else {
            super.onBackPressed()
        }
    }
}