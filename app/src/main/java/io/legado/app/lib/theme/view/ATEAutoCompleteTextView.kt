package io.legado.app.lib.theme.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
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
}
