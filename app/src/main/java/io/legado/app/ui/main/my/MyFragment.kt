package io.legado.app.ui.main.my

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.ui.search.SearchActivity
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity

class MyFragment : BaseFragment(R.layout.fragment_my_config) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = PreferenceFragment()
        childFragmentManager.beginTransaction().replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
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