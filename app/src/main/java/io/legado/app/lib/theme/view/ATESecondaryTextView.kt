package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.lib.theme.secondaryTextColor

/**
 * @author Aidan Follestad (afollestad)
 */
class ATESecondaryTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        setTextColor(context.secondaryTextColor)
    }
}
