package io.legado.app.utils

import android.graphics.Color

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Suppress("unused")
object ColorUtils {

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

    fun isColorLight(@ColorInt color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.4
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


    /**
     * 按条件的到随机颜色
     *
     * @param alpha 透明
     * @param lower 下边界
     * @param upper 上边界
     * @return 颜色值
     */
    fun getRandomColor(alpha: Int, lower: Int, upper: Int): Int {
        return RandomColor(alpha, lower, upper).color
    }

    /**
     * @return 获取随机色
     */
    fun getRandomColor(): Int {
        return RandomColor(255, 80, 200).color
    }


    /**
     * 随机颜色
     */
    class RandomColor(alpha: Int, lower: Int, upper: Int) {
        private var alpha: Int = 0
        private var lower: Int = 0
        private var upper: Int = 0

        //随机数是前闭  后开
        val color: Int
            get() {
                val red = getLower() + Random().nextInt(getUpper() - getLower() + 1)
                val green = getLower() + Random().nextInt(getUpper() - getLower() + 1)
                val blue = getLower() + Random().nextInt(getUpper() - getLower() + 1)

                return Color.argb(getAlpha(), red, green, blue)
            }

        init {
            require(upper > lower) { "must be lower < upper" }
            setAlpha(alpha)
            setLower(lower)
            setUpper(upper)
        }

        private fun getAlpha(): Int {
            return alpha
        }

        private fun setAlpha(alpha: Int) {
            var alpha1 = alpha
            if (alpha1 > 255) alpha1 = 255
            if (alpha1 < 0) alpha1 = 0
            this.alpha = alpha1
        }

        private fun getLower(): Int {
            return lower
        }

        private fun setLower(lower: Int) {
            var lower1 = lower
            if (lower1 < 0) lower1 = 0
            this.lower = lower1
        }

        private fun getUpper(): Int {
            return upper
        }

        private fun setUpper(upper: Int) {
            var upper1 = upper
            if (upper1 > 255) upper1 = 255
            this.upper = upper1
        }
    }

}
