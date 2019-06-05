package io.legado.app.ui.main.myconfig

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.utils.getCompatColor
import kotlinx.android.synthetic.main.view_titlebar.*

class MyConfigFragment : Fragment(R.layout.fragment_my_config), Toolbar.OnMenuItemClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.e("TAG", "MyConfigFragment")
        toolbar.inflateMenu(R.menu.my_config)
        toolbar.menu?.let {
            if (it is MenuBuilder) {
                it.setOptionalIconsVisible(true)
            }
            context?.let { context ->
                val primaryTextColor = getPrimaryTextColor(ColorUtils.isColorLight(ThemeStore.primaryColor(context)))
                val defaultTextColor = getCompatColor(R.color.tv_text_default)
                it.forEach { item ->
                    (item as MenuItemImpl).let { impl ->
                        //overflow：展开的item
                        DrawableUtils.setTint(
                            impl.icon,
                            if (impl.requiresOverflow()) defaultTextColor else primaryTextColor
                        )
                    }
                }
            }
        }
        toolbar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return false
    }

}