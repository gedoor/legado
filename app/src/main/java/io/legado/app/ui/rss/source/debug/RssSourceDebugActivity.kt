package io.legado.app.ui.rss.source.debug

import android.os.Bundle
import android.widget.SearchView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivitySourceDebugBinding
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast


class RssSourceDebugActivity : VMBaseActivity<ActivitySourceDebugBinding, RssSourceDebugModel>() {

    override val viewModel: RssSourceDebugModel
        get() = getViewModel(RssSourceDebugModel::class.java)

    private lateinit var adapter: RssSourceDebugAdapter

    override fun getViewBinding(): ActivitySourceDebugBinding {
        return ActivitySourceDebugBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        viewModel.observe { state, msg ->
            launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    binding.rotateLoading.hide()
                }
            }
        }
        viewModel.initData(intent.getStringExtra("key")) {
            startSearch()
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        adapter = RssSourceDebugAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.rotateLoading.loadingColor = accentColor
    }

    private fun initSearchView() {
        binding.titleBar.findViewById<SearchView>(R.id.search_view).gone()
    }

    private fun startSearch() {
        adapter.clearItems()
        viewModel.startDebug({
            binding.rotateLoading.show()
        }, {
            toast("未获取到源")
        })
    }
}