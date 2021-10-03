package io.legado.app.utils

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
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

/////以下方法需要在View完全被绘制出来之后调用，否则判断不了,在比如 onWindowFocusChanged（）方法中可以得到正确的结果/////

/**
 * 返回NavigationBar
 */
val Activity.navigationBar: View?
    get() {
        val viewGroup = (window.decorView as? ViewGroup) ?: return null
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            val childId = child.id
            if (childId != View.NO_ID
                && resources.getResourceEntryName(childId) == "navigationBarBackground"
            ) {
                return child
            }
        }
        return null
    }

/**
 * 返回NavigationBar是否存在
 */
val Activity.isNavigationBarExist: Boolean
    get() = navigationBar != null

/**
 * 返回NavigationBar高度
 */
val Activity.navigationBarHeight: Int
    get() {
        if (isNavigationBarExist) {
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return resources.getDimensionPixelSize(resourceId)
        }
        return 0
    }

/**
 * 返回navigationBar位置
 */
val Activity.navigationBarGravity: Int
    get() {
        val gravity = (navigationBar?.layoutParams as? FrameLayout.LayoutParams)?.gravity
        return gravity ?: Gravity.BOTTOM
    }
