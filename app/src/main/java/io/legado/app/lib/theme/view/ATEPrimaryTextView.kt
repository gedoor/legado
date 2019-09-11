package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.lib.theme.ThemeStore

/**
 * @author Aidan Follestad (afollestad)
 */
class ATEPrimaryTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        setTextColor(ThemeStore.textColorPrimary(context))
    }
}
