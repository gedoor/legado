@file:Suppress("unused")

package io.legado.app.utils

import android.content.res.ColorStateList
import androidx.annotation.ColorInt
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationView

fun NavigationView.setItemIconColors(
    @ColorInt normalColor: Int,
    @ColorInt selectedColor: Int
) {
    val iconSl = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_checked)
        ),
        intArrayOf(normalColor, selectedColor)
    )
    itemIconTintList = iconSl
}

fun NavigationView.setItemTextColors(
    @ColorInt normalColor: Int,
    @ColorInt selectedColor: Int
) {
    val textSl = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_checked)
        ),
        intArrayOf(normalColor, selectedColor)
    )
    itemTextColor = textSl
}

fun NavigationView.disableScrollbar() {
    val navigationMenuView = getChildAt(0) as? NavigationMenuView
    navigationMenuView?.isVerticalScrollBarEnabled = false
}

