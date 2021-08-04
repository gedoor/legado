package io.legado.app.utils

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowMetrics

fun Activity.getSize(): DisplayMetrics {
    val displayMetrics = DisplayMetrics()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
        val insets = windowMetrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        displayMetrics.widthPixels = windowMetrics.bounds.width() - insets.left - insets.right
        displayMetrics.heightPixels = windowMetrics.bounds.height() - insets.top - insets.bottom
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
    }
    return displayMetrics
}

/**
 * 该方法需要在View完全被绘制出来之后调用，否则判断不了
 * 在比如 onWindowFocusChanged（）方法中可以得到正确的结果
 */
val Activity.navigationBarHeight: Int
    get() {
        if (SystemUtils.isNavigationBarExist(this)) {
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }
        return 0
    }