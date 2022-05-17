package io.legado.app.utils

import android.graphics.Color
import java.util.*

@Suppress("unused")
class RandomColor(alpha: Int, lower: Int, upper: Int) {

    constructor() : this(255, 80, 200)

    private var alpha: Int = 0
    private var lower: Int = 0
    private var upper: Int = 0

    init {
        require(upper > lower) { "must be lower < upper" }
        setAlpha(alpha)
        setLower(lower)
        setUpper(upper)
    }

    //随机数是前闭  后开
    fun build(): Int {
        val red = getLower() + Random().nextInt(getUpper() - getLower() + 1)
        val green = getLower() + Random().nextInt(getUpper() - getLower() + 1)
        val blue = getLower() + Random().nextInt(getUpper() - getLower() + 1)
        return Color.argb(getAlpha(), red, green, blue)
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