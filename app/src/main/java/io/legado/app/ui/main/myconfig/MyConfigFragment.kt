package io.legado.app.ui.main.myconfig

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.ui.search.SearchActivity
import kotlinx.android.synthetic.main.view_titlebar.*
import org.jetbrains.anko.startActivity

class MyConfigFragment : BaseFragment(R.layout.fragment_my_config) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        childFragmentManager.beginTransaction().add(R.id.pre_fragment, PreferenceFragment()).commit()
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