package io.legado.app.ui.sourcedebug

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_source_debug.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast

class SourceDebugActivity : VMBaseActivity<SourceDebugModel>(R.layout.activity_source_debug) {

    override val viewModel: SourceDebugModel
        get() = getViewModel(SourceDebugModel::class.java)

    private lateinit var adapter: SourceDebugAdapter
    private val qrRequestCode = 101

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.init(intent.getStringExtra("key"))
        initRecyclerView()
        initSearchView()
        viewModel.observe{state, msg->
            launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    rotate_loading.hide()
                }
            }
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        adapter = SourceDebugAdapter(this)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
        rotate_loading.loadingColor = accentColor
    }

    private fun initSearchView() {
        search_view.onActionViewExpanded()
        search_view.isSubmitButtonEnabled = true
        search_view.queryHint = getString(R.string.search_book_key)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                search_view.clearFocus()
                startSearch(query ?: "我的")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun startSearch(key: String) {
        adapter.clearItems()
        viewModel.startDebug(key, {
            rotate_loading.show()
        }, {
            toast("未获取到书源")
        })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            qrRequestCode -> {
                if (resultCode == RESULT_OK) {
                    data?.getStringExtra("result")?.let {
                        startSearch(it)
                    }
                }
            }
        }
    }
}