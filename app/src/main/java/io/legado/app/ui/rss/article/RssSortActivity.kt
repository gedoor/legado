package io.legado.app.ui.rss.article

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityRssArtivlesBinding
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.gone
import io.legado.app.utils.visible

class RssSortActivity : VMBaseActivity<ActivityRssArtivlesBinding, RssSortViewModel>() {

    override val viewModel: RssSortViewModel
            by viewModels()
    private var sorts = linkedMapOf<String, String>()
    private lateinit var adapter: TabFragmentPageAdapter
    private val upSourceResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.initData(intent) {
                upFragments()
            }
        }
    }

    override fun getViewBinding(): ActivityRssArtivlesBinding {
        return ActivityRssArtivlesBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        adapter = TabFragmentPageAdapter()
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = sorts.keys.elementAt(position)
        }.attach()
        viewModel.titleLiveData.observe(this, {
            binding.titleBar.title = it
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
                upSourceResult.launch(
                    Intent(this, RssSourceEditActivity::class.java)
                        .putExtra("data", it)
                )
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
        viewModel.rssSource?.sortUrls()?.let {
            sorts = it
        }
        if (sorts.size == 1) {
            binding.tabLayout.gone()
        } else {
            binding.tabLayout.visible()
        }
        adapter.notifyDataSetChanged()
    }

    private inner class TabFragmentPageAdapter : FragmentStateAdapter(this) {

        override fun getItemCount(): Int {
            return sorts.size
        }

        override fun getItemId(position: Int): Long {
            val style = viewModel.rssSource?.articleStyle ?: 0
            return style * 100 + super.getItemId(position)
        }

        override fun createFragment(position: Int): Fragment {
            return RssArticlesFragment.create(
                sorts.keys.elementAt(position),
                sorts.values.elementAt(position)
            )
        }
    }

}