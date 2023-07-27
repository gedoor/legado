package io.legado.app.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Suppress("unused", "MemberVisibilityCanBePrivate")
object ColorUtils {

    fun isColorLight(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) >= 0.5
    }

    fun intToString(intColor: Int): String {
        return String.format("#%06X", 0xFFFFFF and intColor)
    }

    fun stripAlpha(@ColorInt color: Int): Int {
        return -0x1000000 or color
    }

    @ColorInt
    fun shiftColor(@ColorInt color: Int, @FloatRange(from = 0.0, to = 2.0) by: Float): Int {
        if (by == 1f) return color
        val alpha = Color.alpha(color)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= by // value component
        return (alpha shl 24) + (0x00ffffff and Color.HSVToColor(hsv))
    }

    @ColorInt
    fun darkenColor(@ColorInt color: Int): Int {
        return shiftColor(color, 0.9f)
    }

    @ColorInt
    fun lightenColor(@ColorInt color: Int): Int {
        return shiftColor(color, 1.1f)
    }

    @ColorInt
    fun invertColor(@ColorInt color: Int): Int {
        val r = 255 - Color.red(color)
        val g = 255 - Color.green(color)
        val b = 255 - Color.blue(color)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    @ColorInt
    fun adjustAlpha(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    @ColorInt
    fun withAlpha(@ColorInt baseColor: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float): Int {
        val a = min(255, max(0, (alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and baseColor
        return a + rgb
    }

    /**
     * Taken from CollapsingToolbarLayout's CollapsingTextHelper class.
     */
    fun blendColors(color1: Int, color2: Int, @FloatRange(from = 0.0, to = 1.0) ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio
        val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio
        val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio
        val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }

    fun argb(r: Int, g: Int, b: Int): Int {
        return argb(Byte.MAX_VALUE.toInt(), r, g, b)
    }

    fun argb(alpha: Int, r: Int, g: Int, b: Int): Int {
        val colorByteArr =
            byteArrayOf(alpha.toByte(), r.toByte(), g.toByte(), b.toByte())
        return byteArrToInt(colorByteArr)
    }

    fun rgb(argb: Int): IntArray {
        return intArrayOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)
    }

    fun byteArrToInt(colorByteArr: ByteArray): Int {
        return ((colorByteArr[0].toInt() shl 24) + (colorByteArr[1].toInt() and 0xFF shl 16)
                + (colorByteArr[2].toInt() and 0xFF shl 8) + (colorByteArr[3].toInt() and 0xFF))
    }

    /**
     * Computes the difference between two RGB colors by converting them to the L*a*b scale and
     * comparing them using the CIE76 algorithm { http://en.wikipedia.org/wiki/Color_difference#CIE76}
     */
    fun getColorDifference(a: Int, b: Int): Double {
        val lab1 = DoubleArray(3)
        val lab2 = DoubleArray(3)
        ColorUtils.colorToLAB(a, lab1)
        ColorUtils.colorToLAB(b, lab2)
        return sqrt(
            (lab2[0] - lab1[0])
                .pow(2.0) + (lab2[1] - lab1[1])
                .pow(2.0) + (lab2[2] - lab1[2])
                .pow(2.0)
        )
    }
}
