package io.legado.app.ui.rss.source.debug

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import kotlinx.android.synthetic.main.activity_source_debug.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast


class RssSourceDebugActivity : VMBaseActivity<RssSourceDebugModel>(R.layout.activity_source_debug) {

    override val viewModel: RssSourceDebugModel
        get() = getViewModel(RssSourceDebugModel::class.java)

    private lateinit var adapter: RssSourceDebugAdapter
    private val qrRequestCode = 101

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
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
        rotate_loading.loadingColor = accentColor
    }

    private fun initSearchView() {
        search_view.gone()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_debug, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_scan -> {
                startActivityForResult<QrCodeActivity>(qrRequestCode)
            }
        }
        return super.onCompatOptionsItemSelected(item)
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