package io.legado.app.ui.rss.source.debug

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import kotlinx.android.synthetic.main.activity_source_debug.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast


class RssSourceDebugActivity : VMBaseActivity<RssSourceDebugModel>(R.layout.activity_source_debug) {

    override val viewModel: RssSourceDebugModel
        get() = getViewModel(RssSourceDebugModel::class.java)

    private lateinit var adapter: RssSourceDebugAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        viewModel.observe { state, msg ->
            launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    rotate_loading.hide()
                }
            }
        }
        viewModel.initData(intent.getStringExtra("key")) {
            startSearch()
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        adapter = RssSourceDebugAdapter(this)
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
        rotate_loading.loadingColor = accentColor
    }

    private fun initSearchView() {
        search_view.gone()
    }

    private fun startSearch() {
        adapter.clearItems()
        viewModel.startDebug({
            rotate_loading.show()
        }, {
            toast("未获取到源")
        })
    }
}