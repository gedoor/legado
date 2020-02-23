package io.legado.app.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor

open class StrokeTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    init {
        if (isInEditMode) {
            background = Selector.shapeBuild()
                .setCornerRadius(1.dp)
                .setStrokeWidth(1.dp)
                .setDisabledStrokeColor(context.getCompatColor(R.color.md_grey_500))
                .setDefaultStrokeColor(context.getCompatColor(R.color.tv_text_secondary))
                .setSelectedStrokeColor(context.getCompatColor(R.color.colorAccent))
                .setPressedBgColor(context.getCompatColor(R.color.transparent30))
                .create()
            this.setTextColor(
                Selector.colorBuild()
                    .setDefaultColor(context.getCompatColor(R.color.tv_text_secondary))
                    .setSelectedColor(context.getCompatColor(R.color.colorAccent))
                    .setDisabledColor(context.getCompatColor(R.color.md_grey_500))
                    .create()
            )
        } else {
            background = Selector.shapeBuild()
                .setCornerRadius(1.dp)
                .setStrokeWidth(1.dp)
                .setDisabledStrokeColor(context.getCompatColor(R.color.md_grey_500))
                .setDefaultStrokeColor(ThemeStore.textColorSecondary(context))
                .setSelectedStrokeColor(ThemeStore.accentColor(context))
                .setPressedBgColor(context.getCompatColor(R.color.transparent30))
                .create()
            this.setTextColor(
                Selector.colorBuild()
                    .setDefaultColor(ThemeStore.textColorSecondary(context))
                    .setSelectedColor(ThemeStore.accentColor(context))
                    .setDisabledColor(context.getCompatColor(R.color.md_grey_500))
                    .create()
            )
        }
    }
}
