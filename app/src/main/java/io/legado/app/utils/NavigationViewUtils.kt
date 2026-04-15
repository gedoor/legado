@file:Suppress("unused")

package io.legado.app.utils

import android.content.res.ColorStateList
import androidx.annotation.ColorInt
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
    // NavigationView 内部第 0 个子 View 通常是菜单列表容器。
    // 这里不依赖 material 的 internal 类（RestrictedApi），仅通过 View 公共属性关闭纵向滚动条。
    getChildAt(0)?.isVerticalScrollBarEnabled = false
}

