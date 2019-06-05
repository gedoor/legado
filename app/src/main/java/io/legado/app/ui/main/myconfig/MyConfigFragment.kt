package io.legado.app.ui.main.myconfig

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigViewModel
import io.legado.app.utils.initIconColor
import kotlinx.android.synthetic.main.fragment_my_config.*
import kotlinx.android.synthetic.main.view_titlebar.*

class MyConfigFragment : Fragment(R.layout.fragment_my_config), Toolbar.OnMenuItemClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("TAG", "MyConfigFragment")
        toolbar.inflateMenu(R.menu.my_config)
        toolbar.menu.initIconColor
        toolbar.setOnMenuItemClickListener(this)
        tv_theme_config.setOnClickListener(View.OnClickListener {
            val intent = Intent(context, ConfigActivity::class.java)
            intent.putExtra("configType", ConfigViewModel.TYPE_THEME_CONFIG)
            startActivity(intent)
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return false
    }

}