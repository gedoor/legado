package io.legado.app.utils

import android.content.Context
import android.graphics.PorterDuff
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.forEach
import io.legado.app.R
import io.legado.app.lib.theme.DrawableUtils
import io.legado.app.lib.theme.primaryTextColor
import java.lang.reflect.Method
import java.util.*

fun Menu.applyTint(context: Context, inPrimary: Boolean = true): Menu = this.let { menu ->
    if (menu is MenuBuilder) {
        menu.setOptionalIconsVisible(true)
    }
    val primaryTextColor = context.primaryTextColor
    val defaultTextColor = context.getCompatColor(R.color.tv_text_default)
    menu.forEach { item ->
        (item as MenuItemImpl).let { impl ->
            //overflow：展开的item
            DrawableUtils.setTint(
                impl.icon,
                if (!inPrimary) defaultTextColor
                else if (impl.requiresOverflow()) defaultTextColor
                else primaryTextColor
            )
        }
    }
    return menu
}

fun Menu.applyOpenTint(context: Context) {
    //展开菜单显示图标
    if (this.javaClass.simpleName.equals("MenuBuilder", ignoreCase = true)) {
        val defaultTextColor = context.getCompatColor(R.color.tv_text_default)
        try {
            var method: Method =
                this.javaClass.getDeclaredMethod("setOptionalIconsVisible", java.lang.Boolean.TYPE)
            method.isAccessible = true
            method.invoke(this, true)
            method = this.javaClass.getDeclaredMethod("getNonActionItems")
            val menuItems = method.invoke(this)
            if (menuItems is ArrayList<*>) {
                for (menuItem in menuItems) {
                    if (menuItem is MenuItem) {
                        val drawable = menuItem.icon
                        if (drawable != null) {
                            drawable.mutate()
                            drawable.setColorFilter(
                                defaultTextColor,
                                PorterDuff.Mode.SRC_ATOP
                            )
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }
}