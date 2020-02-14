package io.legado.app.ui.widget.text

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore

class AccentBgTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentBgTextView)
        val radios =
            typedArray.getDimensionPixelOffset(R.styleable.AccentBgTextView_bg_radius, 0)
        typedArray.recycle()
        background = Selector.shapeBuild()
            .setCornerRadius(radios)
            .setDefaultBgColor(ThemeStore.accentColor(context))
            .setPressedBgColor(ColorUtils.darkenColor(ThemeStore.accentColor(context)))
            .create()
        setTextColor(Color.WHITE)
    }
}
