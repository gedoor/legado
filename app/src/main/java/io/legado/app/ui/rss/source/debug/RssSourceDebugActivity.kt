package io.legado.app.ui.rss.source.debug

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityRssSourceDebugBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.applyNavigationBarPadding
import io.legado.app.utils.gone
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch
import splitties.views.onClick


class RssSourceDebugActivity : VMBaseActivity<ActivityRssSourceDebugBinding, RssSourceDebugModel>() {

    override val binding by viewBinding(ActivityRssSourceDebugBinding::inflate)
    override val viewModel by viewModels<RssSourceDebugModel>()

    private val adapter by lazy { RssSourceDebugAdapter(this) }
    private val searchView: androidx.appcompat.widget.SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()

        viewModel.initData(intent.getStringExtra("key")) {
            initHelpView()
            initSearchView()
        }
        viewModel.observe { state, msg ->
            lifecycleScope.launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    binding.rotateLoading.gone()
                }
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_source_debug, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_list_src -> showDialogFragment(TextDialog("Html", viewModel.listSrc))
            R.id.menu_content_src -> showDialogFragment(TextDialog("Html", viewModel.contentSrc))
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.applyNavigationBarPadding()
        binding.rotateLoading.loadingColor = accentColor
    }

    private fun initSearchView() {
        openOrCloseHelp(true)
        if (viewModel.rssSource?.searchUrl.isNullOrBlank()) {
            searchView.gone()
            return
        }
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                openOrCloseHelp(false)
                startSearch(query ?: "我的")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            openOrCloseHelp(hasFocus)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun initHelpView() {
        binding.textSort.onClick {
            openOrCloseHelp(false)
            startDebug()
        }
    }
    /**
     * 打开关闭辅助面板
     */
    private fun openOrCloseHelp(open: Boolean) {
        if (open) {
            binding.help.visibility = View.VISIBLE
        } else {
            binding.help.visibility = View.GONE
        }
    }
    private fun startSearch(key: String) {
        adapter.clearItems()
        viewModel.startDebug(key, {
            binding.rotateLoading.visible()
        }, {
            toastOnUi("未获取到书源")
        })
    }

    private fun startDebug() {
        adapter.clearItems()
        viewModel.rssSource?.let {
            binding.rotateLoading.visible()
            viewModel.startDebug(it)
        } ?: toastOnUi(R.string.error_no_source)
    }
}