package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore


class ATEAutoCompleteTextView(context: Context, attrs: AttributeSet) :
    AppCompatAutoCompleteTextView(context, attrs) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            backgroundTintList = Selector.colorBuild()
                .setFocusedColor(ThemeStore.accentColor(context))
                .setDefaultColor(ThemeStore.textColorPrimary(context))
                .create()
        }
    }

    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)

        performFiltering(text, KeyEvent.KEYCODE_UNKNOWN)
    }
}
