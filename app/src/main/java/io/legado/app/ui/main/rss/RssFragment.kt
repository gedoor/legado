package io.legado.app.ui.main.rss

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.data.entities.RssSource
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.cancelButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.article.RssArticlesActivity
import io.legado.app.ui.rss.source.manage.RssSourceActivity
import io.legado.app.utils.startActivity
import kotlinx.android.synthetic.main.fragment_rss.*
import kotlinx.android.synthetic.main.view_title_bar.*

class RssFragment : BaseFragment(R.layout.fragment_rss),
    RssAdapter.CallBack {

    private lateinit var adapter: RssAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_rss, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_rss_config -> startActivity<RssSourceActivity>()
            R.id.menu_rss_add -> {
                alert {
                    title = "快速添加并预览"
                    val layout = LinearLayout(activity)
                    val urlEdit = EditText(activity)
                    urlEdit.hint  = "输入RSS地址"
                    urlEdit.width = 800
                    layout.gravity = Gravity.CENTER
                    layout.addView(urlEdit)
                    customView = layout
                    cancelButton{
                        Log.i("RSS","Quick Add URL cancel")
                    }
                    yesButton{
                        Log.i("RSS","Quick Add URL: ${urlEdit.text}")
                        startActivity<RssArticlesActivity>("QuickAddURL" to urlEdit.text.toString().trim())
                    }
                }.show()
            }
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        adapter = RssAdapter(requireContext(), this)
        recycler_view.layoutManager = GridLayoutManager(requireContext(), 4)
        recycler_view.adapter = adapter
    }

    private fun initData() {
        App.db.rssSourceDao().liveEnabled().observe(viewLifecycleOwner, Observer {
            adapter.setItems(it)
        })
    }

    override fun openRss(rssSource: RssSource) {

    }
}