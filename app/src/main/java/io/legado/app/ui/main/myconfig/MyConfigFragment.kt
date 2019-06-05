package io.legado.app.ui.main.myconfig

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.setIconColor
import kotlinx.android.synthetic.main.view_titlebar.*

class MyConfigFragment : Fragment(R.layout.fragment_my_config), Toolbar.OnMenuItemClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("TAG", "MyConfigFragment")
        toolbar.inflateMenu(R.menu.my_config)
        toolbar.menu.setIconColor(App.INSTANCE)
        toolbar.setOnMenuItemClickListener(this)
        childFragmentManager.beginTransaction().add(R.id.pre_fragment, PreferenceFragment()).commit()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return false
    }

}