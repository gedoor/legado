package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore

class ATETextInputLayout : TextInputLayout {
    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        defaultHintTextColor = Selector.colorBuild().setDefaultColor(ThemeStore.accentColor(context)).create()
    }

    override fun draw(canvas: Canvas) {

        super.draw(canvas)
    }
}
