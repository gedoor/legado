package io.legado.app.utils

import android.content.Context
import io.legado.app.R
import io.legado.app.constant.Theme
import io.legado.app.lib.theme.primaryTextColor

@Suppress("unused")
object UIUtils {

    fun getMenuColor(
        context: Context,
        theme: Theme = Theme.Auto,
        requiresOverflow: Boolean = false
    ): Int {
        val defaultTextColor = context.getCompatColor(R.color.primaryText)
        if (requiresOverflow)
            return defaultTextColor
        val primaryTextColor = context.primaryTextColor
        return when (theme) {
            Theme.Dark -> context.getCompatColor(R.color.md_white_1000)
            Theme.Light -> context.getCompatColor(R.color.md_black_1000)
            else -> primaryTextColor
        }
    }

}