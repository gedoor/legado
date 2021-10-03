package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ATERadioButton(context: Context, attrs: AttributeSet) : AppCompatRadioButton(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
