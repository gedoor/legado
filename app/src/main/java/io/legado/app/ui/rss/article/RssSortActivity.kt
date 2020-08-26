package io.legado.app.ui.rss.article

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_rss_artivles.*
import org.jetbrains.anko.startActivityForResult

class RssSortActivity : VMBaseActivity<RssSortViewModel>(R.layout.activity_rss_artivles) {

    override val viewModel: RssSortViewModel
        get() = getViewModel(RssSortViewModel::class.java)
    private val editSource = 12319
    private val fragments = linkedMapOf<String, RssArticlesFragment>()
    private lateinit var adapter: TabFragmentPageAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        adapter = TabFragmentPageAdapter(supportFragmentManager)
        tab_layout.setupWithViewPager(view_pager)
        view_pager.adapter = adapter
        viewModel.titleLiveData.observe(this, {
            title_bar.title = it
        })
        viewModel.initData(intent) {
            upFragments()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_articles, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit_source -> viewModel.rssSource?.sourceUrl?.let {
                startActivityForResult<RssSourceEditActivity>(editSource, Pair("data", it))
            }
            R.id.menu_clear -> {
                viewModel.url?.let {
                    viewModel.clearArticles()
                }
            }
            R.id.menu_switch_layout -> {
                viewModel.switchLayout()
                upFragments()
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun upFragments() {
        fragments.clear()
        viewModel.rssSource?.sortUrls()?.forEach {
            fragments[it.key] = RssArticlesFragment.create(it.key, it.value)
        }
        if (fragments.size == 1) {
            tab_layout.gone()
        } else {
            tab_layout.visible()
        }
        adapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            editSource -> if (resultCode == Activity.RESULT_OK) {
                viewModel.initData(intent) {
                    upFragments()
                }
            }
        }
    }

    private inner class TabFragmentPageAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragments.keys.elementAt(position)
        }

        override fun getItem(position: Int): Fragment {
            return fragments.values.elementAt(position)
        }

        override fun getCount(): Int {
            return fragments.size
        }
    }

}