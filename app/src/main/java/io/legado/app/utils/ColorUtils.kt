package io.legado.app.utils

import android.graphics.Color

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import java.util.*
import kotlin.math.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
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

    fun argb(R: Int, G: Int, B: Int): Int {
        return argb(Byte.MAX_VALUE.toInt(), R, G, B)
    }

    fun argb(A: Int, R: Int, G: Int, B: Int): Int {
        val colorByteArr =
            byteArrayOf(A.toByte(), R.toByte(), G.toByte(), B.toByte())
        return byteArrToInt(colorByteArr)
    }

    fun rgb(argb: Int): IntArray? {
        return intArrayOf(argb shr 16 and 0xFF, argb shr 8 and 0xFF, argb and 0xFF)
    }

    fun byteArrToInt(colorByteArr: ByteArray): Int {
        return ((colorByteArr[0].toInt() shl 24) + (colorByteArr[1].toInt() and 0xFF shl 16)
                + (colorByteArr[2].toInt() and 0xFF shl 8) + (colorByteArr[3].toInt() and 0xFF))
    }

    fun rgb2lab(R: Int, G: Int, B: Int): IntArray {
        val x: Float
        val y: Float
        val z: Float
        val fx: Float
        val fy: Float
        val fz: Float
        val xr: Float
        val yr: Float
        val zr: Float
        val ls: Float
        val `as`: Float
        val bs: Float
        val eps = 216f / 24389f
        val k = 24389f / 27f
        val xr1 = 0.964221f // reference white D50
        val yr1 = 1.0f
        val zr1 = 0.825211f

        // RGB to XYZ
        var r: Float = R / 255f //R 0..1
        var g: Float = G / 255f //G 0..1
        var b: Float = B / 255f //B 0..1

        // assuming sRGB (D65)
        r = if (r <= 0.04045) r / 12 else ((r + 0.055) / 1.055).pow(2.4).toFloat()
        g = if (g <= 0.04045) g / 12 else ((g + 0.055) / 1.055).pow(2.4).toFloat()
        b = if (b <= 0.04045) b / 12 else ((b + 0.055) / 1.055).pow(2.4).toFloat()
        x = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b
        y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b
        z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b

        // XYZ to Lab
        xr = x / xr1
        yr = y / yr1
        zr = z / zr1
        fx = if (xr > eps) xr.toDouble().pow(1 / 3.0)
            .toFloat() else ((k * xr + 16.0) / 116.0).toFloat()
        fy = if (yr > eps) yr.toDouble().pow(1 / 3.0)
            .toFloat() else ((k * yr + 16.0) / 116.0).toFloat()
        fz = if (zr > eps) zr.toDouble().pow(1 / 3.0)
            .toFloat() else ((k * zr + 16.0) / 116).toFloat()
        ls = 116 * fy - 16
        `as` = 500 * (fx - fy)
        bs = 200 * (fy - fz)
        val lab = IntArray(3)
        lab[0] = (2.55 * ls + .5).toInt()
        lab[1] = (`as` + .5).toInt()
        lab[2] = (bs + .5).toInt()
        return lab
    }

    /**
     * Computes the difference between two RGB colors by converting them to the L*a*b scale and
     * comparing them using the CIE76 algorithm { http://en.wikipedia.org/wiki/Color_difference#CIE76}
     */
    fun getColorDifference(a: Int, b: Int): Double {
        val r1: Int = Color.red(a)
        val g1: Int = Color.green(a)
        val b1: Int = Color.blue(a)
        val r2: Int = Color.red(b)
        val g2: Int = Color.green(b)
        val b2: Int = Color.blue(b)
        val lab1 = rgb2lab(r1, g1, b1)
        val lab2 = rgb2lab(r2, g2, b2)
        return sqrt(
            (lab2[0] - lab1[0].toDouble())
                .pow(2.0) + (lab2[1] - lab1[1].toDouble())
                .pow(2.0) + (lab2[2] - lab1[2].toDouble())
                .pow(2.0)
        )
    }
}
