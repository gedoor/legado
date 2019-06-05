package io.legado.app.utils

import android.content.Context
import android.view.Menu
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.forEach
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.getPrimaryTextColor

fun Menu.setIconColor(context: Context): Menu = this.let { menu ->
    if (menu is MenuBuilder) {
        menu.setOptionalIconsVisible(true)
    }
    val primaryTextColor = context.getPrimaryTextColor(ColorUtils.isColorLight(ThemeStore.primaryColor(context)))
    val defaultTextColor = context.getCompatColor(R.color.tv_text_default)
    menu.forEach { item ->
        (item as MenuItemImpl).let { impl ->
            //overflow：展开的item
            DrawableUtils.setTint(
                impl.icon,
                if (impl.requiresOverflow()) defaultTextColor else primaryTextColor
            )
        }
    }
    return menu
}