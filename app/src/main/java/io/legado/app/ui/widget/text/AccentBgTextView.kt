package io.legado.app.ui.widget.text

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor

class AccentBgTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    private var radios = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccentBgTextView)
        radios = typedArray.getDimensionPixelOffset(R.styleable.AccentBgTextView_radius, radios)
        typedArray.recycle()
        upBackground()
        setTextColor(Color.WHITE)
    }

    fun setRadios(radio: Int) {
        this.radios = radio.dp
        upBackground()
    }

    private fun upBackground() {
        background = if (isInEditMode) {
            Selector.shapeBuild()
                .setCornerRadius(radios)
                .setDefaultBgColor(context.getCompatColor(R.color.colorAccent))
                .setPressedBgColor(ColorUtils.darkenColor(context.getCompatColor(R.color.colorAccent)))
                .create()
        } else {
            Selector.shapeBuild()
                .setCornerRadius(radios)
                .setDefaultBgColor(ThemeStore.accentColor(context))
                .setPressedBgColor(ColorUtils.darkenColor(ThemeStore.accentColor(context)))
                .create()
        }
    }
}
