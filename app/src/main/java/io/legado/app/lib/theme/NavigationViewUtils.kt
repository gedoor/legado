package io.legado.app.lib.theme

import android.content.res.ColorStateList
import androidx.annotation.ColorInt
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationView

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused")
object NavigationViewUtils {

    fun setItemIconColors(navigationView: NavigationView, @ColorInt normalColor: Int, @ColorInt selectedColor: Int) {
        val iconSl = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)),
            intArrayOf(normalColor, selectedColor)
        )
        navigationView.itemIconTintList = iconSl
    }

    fun setItemTextColors(navigationView: NavigationView, @ColorInt normalColor: Int, @ColorInt selectedColor: Int) {
        val textSl = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_checked), intArrayOf(android.R.attr.state_checked)),
            intArrayOf(normalColor, selectedColor)
        )
        navigationView.itemTextColor = textSl
    }

    /**
     * 去掉navigationView的滚动条
     * @param navigationView NavigationView
     */
    fun disableScrollbar(navigationView: NavigationView?) {
        navigationView ?: return
        val navigationMenuView = navigationView.getChildAt(0) as? NavigationMenuView
        navigationMenuView?.isVerticalScrollBarEnabled = false
    }
}
