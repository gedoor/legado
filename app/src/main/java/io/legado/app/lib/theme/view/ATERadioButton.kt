package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor

/**
 * @author Aidan Follestad (afollestad)
 */
class ATERadioButton(context: Context, attrs: AttributeSet) : AppCompatRadioButton(context, attrs) {

    init {
        if (!isInEditMode) {
            ATH.setTint(this@ATERadioButton, context.accentColor)
        }
    }
}
