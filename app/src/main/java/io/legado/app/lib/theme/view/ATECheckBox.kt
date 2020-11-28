package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor

/**
 * @author Aidan Follestad (afollestad)
 */
class ATECheckBox(context: Context, attrs: AttributeSet) : AppCompatCheckBox(context, attrs) {

    init {
        if (!isInEditMode) {
            ATH.setTint(this, context.accentColor)
        }
    }
}
