package io.legado.app.ui.main.myconfig

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigViewModel
import io.legado.app.ui.search.SearchActivity
import kotlinx.android.synthetic.main.fragment_my_config.*
import kotlinx.android.synthetic.main.view_titlebar.*
import org.jetbrains.anko.startActivity

class MyConfigFragment : BaseFragment(R.layout.fragment_my_config) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)

        tv_theme_config.setOnClickListener {
            requireContext().startActivity<ConfigActivity>(Pair("configType", ConfigViewModel.TYPE_THEME_CONFIG))
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.my_config, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.action_settings -> requireContext().startActivity<SearchActivity>()
        }
    }
}