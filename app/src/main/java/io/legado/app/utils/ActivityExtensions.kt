package io.legado.app.utils

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment

inline fun <reified T : DialogFragment> AppCompatActivity.showDialogFragment(
    arguments: Bundle.() -> Unit = {}
) {
    val dialog = T::class.java.newInstance()
    val bundle = Bundle()
    bundle.apply(arguments)
    dialog.arguments = bundle
    dialog.show(supportFragmentManager, T::class.simpleName)
}

fun AppCompatActivity.showDialogFragment(dialogFragment: DialogFragment) {
    dialogFragment.show(supportFragmentManager, dialogFragment::class.simpleName)
}

val Activity.windowSize: DisplayMetrics
    get() {
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
 * 返回NavigationBar是否存在
 * 该方法需要在View完全被绘制出来之后调用，否则判断不了
 * 在比如 onWindowFocusChanged（）方法中可以得到正确的结果
 */
val Activity.isNavigationBarExist: Boolean
    get() {
        val viewGroup = (window.decorView as? ViewGroup) ?: return false
        for (i in 0 until viewGroup.childCount) {
            val childId = viewGroup.getChildAt(i).id
            if (childId != View.NO_ID
                && resources.getResourceEntryName(childId) == "navigationBarBackground"
            ) {
                return true
            }
        }
        return false
    }

/**
 * 该方法需要在View完全被绘制出来之后调用，否则判断不了
 * 在比如 onWindowFocusChanged（）方法中可以得到正确的结果
 */
val Activity.navigationBarHeight: Int
    get() {
        if (isNavigationBarExist) {
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }
        return 0
    }