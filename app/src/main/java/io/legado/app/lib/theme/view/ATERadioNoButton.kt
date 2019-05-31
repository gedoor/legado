package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.dp

/**
 * @author Aidan Follestad (afollestad)
 */
class ATERadioNoButton : AppCompatRadioButton {

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        background = Selector.shapeBuild()
            .setCornerRadius(3.dp)
            .setStrokeWidth(3.dp)
            .setCheckedBgColor(ThemeStore.accentColor(context))
            .setCheckedStrokeColor(ThemeStore.accentColor(context))
            .setDefaultStrokeColor(Color.WHITE)
            .create()
    }
}
