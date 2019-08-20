package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.dp

class ATEAccentBgTextView : AppCompatTextView {
    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        background = Selector.shapeBuild()
            .setCornerRadius(3.dp)
            .setDefaultBgColor(ThemeStore.accentColor(context))
            .setPressedBgColor(ColorUtils.darkenColor(ThemeStore.accentColor(context)))
            .create()
        setTextColor(Color.WHITE)
    }
}
