package io.legado.app.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.lib.theme.accentColor
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textColorResource

class AccentTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    init {
        if (!isInEditMode) {
            textColor = context.accentColor
        } else {
            textColorResource = R.color.accent
        }
    }

}
