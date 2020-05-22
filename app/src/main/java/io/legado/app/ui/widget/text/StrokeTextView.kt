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

    private var radius = 1.dp

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.StrokeTextView)
        radius = typedArray.getDimensionPixelOffset(R.styleable.StrokeTextView_radius, radius)
        typedArray.recycle()
        upBackground()
    }

    fun setRadius(radius: Int) {
        this.radius = radius.dp
        upBackground()
    }

    private fun upBackground() {
        if (isInEditMode) {
            background = Selector.shapeBuild()
                .setCornerRadius(radius)
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
                .setCornerRadius(radius)
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
