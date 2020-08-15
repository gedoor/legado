package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.legado.app.R
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor

/**
 * @author Aidan Follestad (afollestad)
 */
class ATERadioNoButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {

    init {
        background = Selector.shapeBuild()
            .setCornerRadius(2.dp)
            .setStrokeWidth(2.dp)
            .setCheckedBgColor(ThemeStore.accentColor(context))
            .setCheckedStrokeColor(ThemeStore.accentColor(context))
            .setDefaultStrokeColor(context.getCompatColor(R.color.primaryText))
            .create()
    }
}
