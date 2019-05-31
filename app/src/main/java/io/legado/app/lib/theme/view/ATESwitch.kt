package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Switch
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore

/**
 * @author Aidan Follestad (afollestad)
 */
class ATESwitch : Switch {

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        ATH.setTint(this, ThemeStore.accentColor(context))
    }

    override fun isShown(): Boolean {
        return parent != null && visibility == View.VISIBLE
    }
}
