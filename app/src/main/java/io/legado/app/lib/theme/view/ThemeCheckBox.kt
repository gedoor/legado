package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.applyTint

class ThemeCheckBox(context: Context, attrs: AttributeSet) : AppCompatCheckBox(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
