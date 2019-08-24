package io.legado.app.ui.readbook

import android.view.View
import android.view.Window
import io.legado.app.App
import io.legado.app.utils.getPrefBoolean

object Help {

    fun upSystemUiVisibility(window: Window, hide: Boolean = true) {
        var flag = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        val hideNavigationBar = App.INSTANCE.getPrefBoolean("hideNavigationBar")
        if (hideNavigationBar) {
            flag =
                flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        if (hide) {
            if (App.INSTANCE.getPrefBoolean("hideStatusBar")) {
                flag = flag or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
            if (hideNavigationBar) {
                flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
        window.decorView.systemUiVisibility = flag
    }

}