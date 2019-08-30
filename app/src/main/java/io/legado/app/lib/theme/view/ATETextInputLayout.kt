package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore

class ATETextInputLayout : TextInputLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        defaultHintTextColor = Selector.colorBuild().setDefaultColor(ThemeStore.accentColor(context)).create()
    }

}
