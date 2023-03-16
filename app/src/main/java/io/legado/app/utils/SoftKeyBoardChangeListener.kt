package io.legado.app.utils

import android.graphics.Rect
import android.view.ViewTreeObserver
import android.view.Window
import splitties.systemservices.windowManager
import kotlin.math.abs

class SoftKeyBoardChangeListener : ViewTreeObserver.OnGlobalLayoutListener {

    private var window: Window? = null
    private var softKeyBoardChangeCallback: ((Boolean) -> Unit)? = null
    var keyBoardShowing = false
        private set
    var keyBoardHeight = 0

    fun attach(window: Window, callback: ((Boolean) -> Unit)) {
        this.window = window
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
        softKeyBoardChangeCallback = callback
    }

    fun unAttach() {
        window?.decorView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        window = null
    }

    override fun onGlobalLayout() {
        window?.let {
            val rect = Rect()
            // 获取当前页面窗口的显示范围
            it.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = windowManager.windowSize.heightPixels
            val keyboardHeight = screenHeight - rect.bottom // 输入法的高度
            if (abs(keyboardHeight) > screenHeight / 5) {
                keyBoardShowing = true // 超过屏幕五分之一则表示弹出了输入法
                this.keyBoardHeight = abs(keyboardHeight)
                softKeyBoardChangeCallback?.invoke(true)
            } else {
                keyBoardShowing = false
                this.keyBoardHeight = 0
                softKeyBoardChangeCallback?.invoke(false)
            }
        }
    }

}