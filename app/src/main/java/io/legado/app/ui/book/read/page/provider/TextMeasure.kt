package io.legado.app.ui.book.read.page.provider

import android.text.TextPaint
import android.util.SparseArray
import androidx.core.util.getOrDefault
import java.util.BitSet
import kotlin.math.ceil

class TextMeasure(private var paint: TextPaint) {

    private var chineseCommonWidth = paint.measureText("一")
    private val chineseCommonWidthBitSet = BitSet()
    private val asciiWidths = FloatArray(128) { -1f }
    private val codePointWidths = SparseArray<Float>()

    private fun measureCodePoint(codePoint: Int): Float {
        if (codePoint < 128) {
            return asciiWidths[codePoint]
        }
        if (chineseCommonWidthBitSet[codePoint]) {
            return chineseCommonWidth
        }
        return codePointWidths.getOrDefault(codePoint, -1f)
    }

    private fun measureCodePoints(codePoints: List<Int>) {
        val charArray = String(codePoints.toIntArray(), 0, codePoints.size).toCharArray()
        val widths = FloatArray(charArray.size)
        paint.getTextWidths(charArray, 0, charArray.size, widths)
        val widthsList = ArrayList<Float>(charArray.size)
        val buf = IntArray(1)
        for (i in charArray.indices) {
            if (charArray[i].isLowSurrogate()) continue
            val width = ceil(widths[i])
            widthsList.add(width)
            if (width == 0f && widthsList.size > 0) {
                val lastIndex = widthsList.lastIndex
                buf[0] = codePoints[lastIndex - 1]
                widthsList[lastIndex - 1] = paint.measureText(String(buf, 0, 1))
                buf[0] = codePoints[lastIndex]
                widthsList[lastIndex] = paint.measureText(String(buf, 0, 1))
            }
        }
        for (i in codePoints.indices) {
            val codePoint = codePoints[i]
            val width = widthsList[i]
            if (codePoint < 128) {
                asciiWidths[codePoint] = width
            } else if (width == chineseCommonWidth) {
                chineseCommonWidthBitSet.set(codePoint)
            } else {
                codePointWidths[codePoint] = width
            }
        }
    }


    fun measureTextSplit(text: String): Pair<ArrayList<String>, ArrayList<Float>> {
        var needMeasureCodePoints: HashSet<Int>? = null
        val codePoints = text.toCodePoints()
        val size = codePoints.size
        val widths = ArrayList<Float>(size)
        val stringList = ArrayList<String>(size)
        val buf = IntArray(1)
        for (i in codePoints.indices) {
            val codePoint = codePoints[i]
            val width = measureCodePoint(codePoint)
            widths.add(width)
            if (width == -1f) {
                if (needMeasureCodePoints == null) {
                    needMeasureCodePoints = hashSetOf()
                }
                needMeasureCodePoints.add(codePoint)
            }
            buf[0] = codePoint
            stringList.add(String(buf, 0, 1))
        }
        if (!needMeasureCodePoints.isNullOrEmpty()) {
            measureCodePoints(needMeasureCodePoints.toList())
            for (i in codePoints.indices) {
                if (widths[i] == -1f) {
                    widths[i] = measureCodePoint(codePoints[i])
                }
            }
        }
        return stringList to widths
    }

    fun measureText(text: String): Float {
        var textWidth = 0f
        var needMeasureCodePoints: ArrayList<Int>? = null
        val codePoints = text.toCodePoints()
        for (i in codePoints.indices) {
            val codePoint = codePoints[i]
            val width = measureCodePoint(codePoint)
            if (width == -1f) {
                if (needMeasureCodePoints == null) {
                    needMeasureCodePoints = ArrayList()
                }
                needMeasureCodePoints.add(codePoint)
                continue
            }
            textWidth += width
        }
        if (!needMeasureCodePoints.isNullOrEmpty()) {
            measureCodePoints(needMeasureCodePoints.toHashSet().toList())
            for (i in needMeasureCodePoints.indices) {
                textWidth += measureCodePoint(needMeasureCodePoints[i])
            }
        }
        return textWidth
    }

    private fun String.toCodePoints(): List<Int> {
        val codePoints = ArrayList<Int>(length)
        val charArray = toCharArray()
        val size = length
        var i = 0
        while (i < size) {
            val c1 = charArray[i++]
            var cp = c1.code
            if (c1.isHighSurrogate() && i < size) {
                val c2 = charArray[i]
                if (c2.isLowSurrogate()) {
                    i++
                    cp = Character.toCodePoint(c1, c2)
                }
            }
            codePoints.add(cp)
        }
        return codePoints
    }

    fun setPaint(paint: TextPaint) {
        this.paint = paint
        invalidate()
    }

    private fun invalidate() {
        chineseCommonWidth = paint.measureText("一")
        chineseCommonWidthBitSet.clear()
        codePointWidths.clear()
        asciiWidths.fill(-1f)
    }

}
