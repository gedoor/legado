package io.legado.app.utils

import android.view.Menu
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.forEach
import io.legado.app.App
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.getPrimaryTextColor

val Menu.initIconColor: Menu
    get() = this.let { menu ->
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        val primaryTextColor =
            App.INSTANCE.getPrimaryTextColor(ColorUtils.isColorLight(ThemeStore.primaryColor(App.INSTANCE)))
        val defaultTextColor = App.INSTANCE.getCompatColor(R.color.tv_text_default)
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