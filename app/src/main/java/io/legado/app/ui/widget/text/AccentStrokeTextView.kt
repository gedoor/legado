package io.legado.app.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor

class AccentStrokeTextView(context: Context, attrs: AttributeSet) :
    AppCompatTextView(context, attrs) {

    init {
        background = Selector.shapeBuild()
            .setCornerRadius(3.dp)
            .setStrokeWidth(1.dp)
            .setDisabledStrokeColor(context.getCompatColor(R.color.md_grey_500))
            .setDefaultStrokeColor(ThemeStore.accentColor(context))
            .setPressedBgColor(context.getCompatColor(R.color.transparent30))
            .create()
        setTextColor(
            Selector.colorBuild()
                .setDefaultColor(ThemeStore.accentColor(context))
                .setDisabledColor(context.getCompatColor(R.color.md_grey_500))
                .create()
        )
    }
}
