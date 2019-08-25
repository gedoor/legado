package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor

/**
 * @author Aidan Follestad (afollestad)
 */
class ATESeekBar : AppCompatSeekBar {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        ATH.setTint(this, context.accentColor)
    }
}
