@file:Suppress("DEPRECATION")

package io.legado.app.ui.rss.article

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityRssArtivlesBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RssSortActivity : VMBaseActivity<ActivityRssArtivlesBinding, RssSortViewModel>() {

    override val binding by viewBinding(ActivityRssArtivlesBinding::inflate)
    override val viewModel by viewModels<RssSortViewModel>()
    private val adapter by lazy { TabFragmentPageAdapter() }
    private val sortList = mutableListOf<Pair<String, String>>()
    private val fragmentMap = hashMapOf<String, Fragment>()
    private val editSourceResult = registerForActivityResult(
        StartActivityContract(RssSourceEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.initData(intent) {
                upFragments()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.viewPager.adapter = adapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        binding.tabLayout.setSelectedTabIndicatorColor(accentColor)
        viewModel.titleLiveData.observe(this) {
            binding.titleBar.title = it
        }
        viewModel.initData(intent) {
            upFragments()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_articles, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible =
            !viewModel.rssSource?.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_login -> startActivity<SourceLoginActivity> {
                putExtra("type", "rssSource")
                putExtra("key", viewModel.rssSource?.sourceUrl)
            }
            R.id.menu_refresh_sort -> viewModel.clearSortCache { upFragments() }
            R.id.menu_set_source_variable -> setSourceVariable()
            R.id.menu_edit_source -> viewModel.rssSource?.sourceUrl?.let {
                editSourceResult.launch {
                    putExtra("sourceUrl", it)
                }
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
        launch {
            viewModel.rssSource?.sortUrls()?.let {
                sortList.clear()
                sortList.addAll(it)
            }
            if (sortList.size == 1) {
                binding.tabLayout.gone()
            } else {
                binding.tabLayout.visible()
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun setSourceVariable() {
        launch {
            val source = viewModel.rssSource
            if (source == null) {
                toastOnUi("源不存在")
                return@launch
            }
            val variable = withContext(Dispatchers.IO) { source.getVariable() }
            alert(R.string.set_source_variable) {
                setMessage(source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取"))
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "source variable"
                    editView.setText(variable)
                }
                customView { alertBinding.root }
                okButton {
                    viewModel.rssSource?.setVariable(alertBinding.editView.text?.toString())
                }
                cancelButton()
                neutralButton(R.string.delete) {
                    viewModel.rssSource?.setVariable(null)
                }
            }
        }
    }

    private inner class TabFragmentPageAdapter :
        FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItemPosition(`object`: Any): Int {
            return POSITION_NONE
        }

        override fun getPageTitle(position: Int): CharSequence {
            return sortList[position].first
        }

        override fun getItem(position: Int): Fragment {
            val sort = sortList[position]
            return RssArticlesFragment(sort.first, sort.second)
        }

        override fun getCount(): Int {
            return sortList.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment = super.instantiateItem(container, position) as Fragment
            fragmentMap[sortList[position].first] = fragment
            return fragment
        }
    }

}