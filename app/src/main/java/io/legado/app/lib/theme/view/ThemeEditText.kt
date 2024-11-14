package io.legado.app.lib.theme.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.applyTint

class ThemeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            isLocalePreferredLineHeightForMinimumUsed = false
        }
    }
}
