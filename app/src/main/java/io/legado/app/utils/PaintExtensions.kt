package io.legado.app.utils

import android.os.Build
import android.text.TextPaint

val TextPaint.textHeight: Float
    get() = fontMetrics.run { descent - ascent + leading }

fun TextPaint.getTextWidthsCompat(text: String, widths: FloatArray) {
    getTextWidths(text, widths)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        val letterSpacing = letterSpacing * textSize
        val letterSpacingHalf = letterSpacing * 0.5f
        widths[0] += letterSpacingHalf
        widths[text.lastIndex] += letterSpacingHalf
    }
}
