package io.legado.app.ui.book.read.config

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor
import org.jetbrains.anko.sdk27.listeners.onClick

class ChineseConverter(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {

    private val spannableString = SpannableString("简/繁")
    private var enabledSpan: ForegroundColorSpan = ForegroundColorSpan(context.accentColor)
    private var onChanged: (() -> Unit)? = null

    init {
        background = Selector.shapeBuild()
            .setCornerRadius(1.dp)
            .setStrokeWidth(1.dp)
            .setDisabledStrokeColor(context.getCompatColor(R.color.md_grey_500))
            .setDefaultStrokeColor(ThemeStore.textColorSecondary(context))
            .setSelectedStrokeColor(ThemeStore.accentColor(context))
            .setPressedBgColor(context.getCompatColor(R.color.transparent30))
            .create()
        setTextColor(
            Selector.colorBuild()
                .setDefaultColor(ThemeStore.textColorSecondary(context))
                .setSelectedColor(ThemeStore.accentColor(context))
                .setDisabledColor(context.getCompatColor(R.color.md_grey_500))
                .create()
        )
        text = spannableString
        if (!isInEditMode) {
            upUi(AppConfig.chineseConverterType)
        }
        onClick {
            selectType()
        }
    }

    private fun upUi(type: Int) {
        spannableString.removeSpan(enabledSpan)
        when (type) {
            1 -> spannableString.setSpan(enabledSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            2 -> spannableString.setSpan(enabledSpan, 2, 3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        text = spannableString
    }

    private fun selectType() {
        context.alert(titleResource = R.string.chinese_converter) {
            items(context.resources.getStringArray(R.array.chinese_mode).toList()) { _, i ->
                AppConfig.chineseConverterType = i
                upUi(i)
                onChanged?.invoke()
            }
        }.show()
    }

    fun onChanged(unit: () -> Unit) {
        onChanged = unit
    }
}