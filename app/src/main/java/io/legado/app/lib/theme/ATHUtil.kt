package io.legado.app.lib.theme

import android.content.Context
import androidx.annotation.AttrRes

/**
 * @author Aidan Follestad (afollestad)
 */
object ATHUtil {

    fun isWindowBackgroundDark(context: Context): Boolean {
        return !ColorUtil.isColorLight(ATHUtil.resolveColor(context, android.R.attr.windowBackground))
    }

    @JvmOverloads
    fun resolveColor(context: Context, @AttrRes attr: Int, fallback: Int = 0): Int {
        val a = context.theme.obtainStyledAttributes(intArrayOf(attr))
        try {
            return a.getColor(0, fallback)
        } finally {
            a.recycle()
        }
    }
}