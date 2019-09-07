package io.legado.app.ui.readbook

import android.app.Activity
import android.view.View
import android.view.View.NO_ID
import android.view.ViewGroup
import io.legado.app.App
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.isTransparentStatusBar


object Help {

    private const val NAVIGATION = "navigationBarBackground"

    fun upSystemUiVisibility(activity: Activity, toolBarHide: Boolean = true) {
        var flag = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        val hideNavigationBar = App.INSTANCE.getPrefBoolean("hideNavigationBar")
        if (hideNavigationBar) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        if (toolBarHide) {
            if (App.INSTANCE.getPrefBoolean("hideStatusBar")) {
                flag = flag or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
            if (hideNavigationBar) {
                flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
        activity.window.decorView.systemUiVisibility = flag
        if (toolBarHide) {
            ATH.setLightStatusBar(activity, ReadBookConfig.getConfig().statusIconDark())
        } else {
            ATH.setLightStatusBarAuto(
                activity,
                ThemeStore.statusBarColor(activity, activity.isTransparentStatusBar)
            )
        }
    }

    /**
     * 返回NavigationBar是否存在
     * 该方法需要在View完全被绘制出来之后调用，否则判断不了
     * 在比如 onWindowFocusChanged（）方法中可以得到正确的结果
     */
    fun isNavigationBarExist(activity: Activity?): Boolean {
        activity?.let {
            val vp = it.window.decorView as? ViewGroup
            if (vp != null) {
                for (i in 0 until vp.childCount) {
                    vp.getChildAt(i).context.packageName
                    if (vp.getChildAt(i).id != NO_ID && NAVIGATION == activity.resources.getResourceEntryName(
                            vp.getChildAt(i).id
                        )
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

}