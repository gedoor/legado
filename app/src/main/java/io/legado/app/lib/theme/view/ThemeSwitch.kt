package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.applyTint

/**
 * @author Aidan Follestad (afollestad)
 */
class ThemeSwitch(context: Context, attrs: AttributeSet) : SwitchCompat(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }

    }

}
