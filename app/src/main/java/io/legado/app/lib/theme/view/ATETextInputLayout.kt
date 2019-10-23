package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore

class ATETextInputLayout(context: Context, attrs: AttributeSet?) : TextInputLayout(context, attrs) {

    init {
        defaultHintTextColor = Selector.colorBuild().setDefaultColor(ThemeStore.accentColor(context)).create()
    }

}
