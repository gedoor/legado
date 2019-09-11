package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore

/**
 * @author Aidan Follestad (afollestad)
 */
class ATEEditText(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {

    init {
        ATH.setTint(this, ThemeStore.accentColor(context))
    }
}
