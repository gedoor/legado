package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import io.legado.app.R
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor

/**
 * @author Aidan Follestad (afollestad)
 */
class ATERadioNoButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {

    private val isBottomBackground: Boolean

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ATERadioNoButton)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.ATERadioNoButton_isBottomBackground, false)
        typedArray.recycle()
        initTheme()
    }

    private fun initTheme() {
        when {
            isInEditMode -> Unit
            isBottomBackground -> {
                val isLight = ColorUtils.isColorLight(context.bottomBackground)
                val textColor = context.getPrimaryTextColor(isLight)
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dp)
                    .setStrokeWidth(2.dp)
                    .setCheckedBgColor(context.accentColor)
                    .setCheckedStrokeColor(context.accentColor)
                    .setDefaultStrokeColor(textColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(textColor)
                        .setCheckedColor(context.getPrimaryTextColor(ColorUtils.isColorLight(context.accentColor)))
                        .create()
                )
            }
            else -> {
                val textColor = context.getCompatColor(R.color.primaryText)
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dp)
                    .setStrokeWidth(2.dp)
                    .setCheckedBgColor(context.accentColor)
                    .setCheckedStrokeColor(context.accentColor)
                    .setDefaultStrokeColor(textColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(textColor)
                        .setCheckedColor(context.getPrimaryTextColor(ColorUtils.isColorLight(context.accentColor)))
                        .create()
                )
            }
        }

    }

}
