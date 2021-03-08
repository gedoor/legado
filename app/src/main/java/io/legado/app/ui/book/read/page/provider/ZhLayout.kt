package io.legado.app.ui.book.read.page.provider

import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import io.legado.app.utils.toStringArray
import kotlin.math.max

/*
* 针对中文的断行排版处理-by hoodie13
* 因为StaticLayout对标点处理不符合国人习惯，继承Layout
* 接口封的不抽象，数组用的也琐碎，因目前语法不熟悉，后面完善。
* */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class ZhLayout(
    text: String,
    textPaint: TextPaint,
    width: Int
) : Layout(text, textPaint, width, Alignment.ALIGN_NORMAL, 0f, 0f) {
    var lineStart = IntArray(100)
    var lineWidth = FloatArray(100)
    var lineCompressMod = IntArray(100)
    private var lineCount = 0
    private val curPaint = textPaint
    private val cnCharWitch = getDesiredWidth("我", textPaint)

    enum class BreakMod { NORMAL, BREAK_ONE_CHAR, BREAK_MORE_CHAR, CPS_1, CPS_2, CPS_3, }
    class Locate {
        var start: Float = 0f
        var end: Float = 0f
    }

    class Interval {
        var total: Float = 0f
        var single: Float = 0f
    }

    init {
        var line = 0
        val words = text.toStringArray()
        var lineW = 0f
        var cwPre = 0f

        words.forEachIndexed { index, s ->
            val cw = getDesiredWidth(s, curPaint)
            var breakMod: BreakMod
            var breakLine = false
            lineW += cw
            var offset = 0f
            var breakCharCnt = 0

            if (lineW > width) {
                /*禁止在行尾的标点处理*/
                breakMod = if (index >= 1 && isPrePanc(words[index - 1])) {
                    if (index >= 2 && isPrePanc(words[index - 2])) BreakMod.CPS_2//如果后面还有一个禁首标点则异常
                    else BreakMod.BREAK_ONE_CHAR //无异常场景
                }
                /*禁止在行首的标点处理*/
                else if (isPostPanc(words[index])) {
                    if (index >= 1 && isPostPanc(words[index - 1])) BreakMod.CPS_1//如果后面还有一个禁首标点则异常，不过三个连续行尾标点的用法不通用
                    else if (index >= 2 && isPrePanc(words[index - 2])) BreakMod.CPS_3//如果后面还有一个禁首标点则异常
                    else BreakMod.BREAK_ONE_CHAR //无异常场景
                } else {
                    BreakMod.NORMAL //无异常场景
                }

                /*判断上述逻辑解决不了的特殊情况*/
                var reCheck = false
                var breakIndex = 0
                if (breakMod == BreakMod.CPS_1 &&
                    (inCompressible(words[index]) || inCompressible(words[index - 1]))
                ) reCheck = true
                if (breakMod == BreakMod.CPS_2 &&
                    (inCompressible(words[index - 1]) || inCompressible(words[index - 2]))
                ) reCheck = true
                if (breakMod == BreakMod.CPS_3 &&
                    (inCompressible(words[index]) || inCompressible(words[index - 2]))
                ) reCheck = true
                if (breakMod > BreakMod.BREAK_MORE_CHAR
                    && index < words.lastIndex && isPostPanc(words[index + 1])
                ) reCheck = true

                /*特殊标点使用难保证显示效果，所以不考虑间隔，直接查找到能满足条件的分割字*/
                if (reCheck && index > 2) {
                    breakMod = BreakMod.NORMAL
                    for (i in (index) downTo 1) {
                        if (i == index) {
                            breakIndex = 0
                            cwPre = 0f
                        } else {
                            breakIndex++
                            cwPre += StaticLayout.getDesiredWidth(words[i], textPaint)
                        }
                        if (!isPostPanc(words[i]) && !isPrePanc(words[i - 1])) {
                            breakMod = BreakMod.BREAK_MORE_CHAR
                            break
                        }
                    }
                }

                when (breakMod) {
                    BreakMod.NORMAL -> {//模式0 正常断行
                        offset = cw
                        lineStart[line + 1] = index
                        lineCompressMod[line] = 0
                        breakCharCnt = 1
                    }
                    BreakMod.BREAK_ONE_CHAR -> {//模式1 当前行下移一个字
                        offset = cw + cwPre
                        lineStart[line + 1] = index - 1
                        lineCompressMod[line] = 0
                        breakCharCnt = 2
                    }
                    BreakMod.BREAK_MORE_CHAR -> {//模式2 当前行下移多个字
                        offset = cw + cwPre
                        lineStart[line + 1] = index - breakIndex
                        lineCompressMod[line] = 0
                        breakCharCnt = breakIndex + 1
                    }
                    BreakMod.CPS_1 -> {//模式3 两个后置标点压缩
                        offset = 0f
                        lineStart[line + 1] = index + 1
                        lineCompressMod[line] = 1
                        breakCharCnt = 0
                    }
                    BreakMod.CPS_2 -> { //模式4 前置标点压缩+前置标点压缩+字
                        offset = 0f
                        lineStart[line + 1] = index + 1
                        lineCompressMod[line] = 2
                        breakCharCnt = 0
                    }
                    BreakMod.CPS_3 -> {//模式5 前置标点压缩+字+后置标点压缩
                        offset = 0f
                        lineStart[line + 1] = index + 1
                        lineCompressMod[line] = 3
                        breakCharCnt = 0
                    }
                }
                breakLine = true
            }

            /*当前行写满情况下的断行*/
            if (breakLine) {
                lineWidth[line] = lineW - offset
                lineW = offset
                line++
                addLineArray(line)
            }
            /*已到最后一个字符*/
            if ((words.lastIndex) == index) {
                if (!breakLine) {
                    offset = 0f
                    lineStart[line + 1] = index + 1
                    lineWidth[line] = lineW - offset
                    lineW = offset
                    line++
                    addLineArray(line)
                }
                /*写满断行、段落末尾、且需要下移字符，这种特殊情况下要额外多一行*/
                else if (breakCharCnt > 0) {
                    lineStart[line + 1] = lineStart[line] + breakCharCnt
                    lineWidth[line] = lineW
                    line++
                    addLineArray(line)
                }
            }
            cwPre = cw
        }

        lineCount = line

    }

    private fun addLineArray(line: Int) {
        if (lineStart.size <= line) {
            lineStart = lineStart.copyOf(line + 100)
            lineWidth = lineWidth.copyOf(line + 100)
            lineCompressMod = lineCompressMod.copyOf(line + 100)
        }
    }

    private fun isPostPanc(string: String): Boolean {
        val panc = arrayOf(
            "，", "。", "：", "？", "！", "、", "”", "’", "）", "》", "}",
            "】", ")", ">", "]", "}", ",", ".", "?", "!", ":", "」", "；", ";"
        )
        panc.forEach {
            if (it == string) return true
        }
        return false
    }

    private fun isPrePanc(string: String): Boolean {
        val panc = arrayOf("“", "（", "《", "【", "‘", "‘", "(", "<", "[", "{", "「")
        panc.forEach {
            if (it == string) return true
        }
        return false
    }

    private fun inCompressible(string: String): Boolean {
        return getDesiredWidth(string, curPaint) < cnCharWitch
    }

    private val gap = (cnCharWitch / 12.75).toFloat()
    private fun getPostPancOffset(string: String): Float {
        val textRect = Rect()
        curPaint.getTextBounds(string, 0, 1, textRect)
        return max(textRect.left.toFloat() - gap, 0f)
    }

    private fun getPrePancOffset(string: String): Float {
        val textRect = Rect()
        curPaint.getTextBounds(string, 0, 1, textRect)
        val d = max(cnCharWitch - textRect.right.toFloat() - gap, 0f)
        return cnCharWitch / 2 - d
    }

    fun getDesiredWidth(sting: String, paint: TextPaint) = paint.measureText(sting)

    override fun getLineCount(): Int {
        return lineCount
    }

    override fun getLineTop(line: Int): Int {
        return 0
    }

    override fun getLineDescent(line: Int): Int {
        return 0
    }

    override fun getLineStart(line: Int): Int {
        return lineStart[line]
    }

    override fun getParagraphDirection(line: Int): Int {
        return 0
    }

    override fun getLineContainsTab(line: Int): Boolean {
        return true
    }

    override fun getLineDirections(line: Int): Directions? {
        return null
    }

    override fun getTopPadding(): Int {
        return 0
    }

    override fun getBottomPadding(): Int {
        return 0
    }

    override fun getLineWidth(line: Int): Float = lineWidth[line]

    override fun getEllipsisStart(line: Int): Int {
        return 0
    }

    override fun getEllipsisCount(line: Int): Int {
        return 0
    }

    fun getDefaultWidth(): Float = cnCharWitch

    /*
    * @fun：获取当前行的平均间隔：用于两端对齐，获取左对齐时的右边间隔：用于间隔过大时不再两端对齐
    * @in：行，当前字符串，最大显示宽度
    * @out：单个字符的平均间隔，左对齐的最大间隔
    */
    fun getInterval(line: Int, words: Array<String>, visibleWidth: Int): Interval {
        val interval = Interval()
        val total: Float
        val d: Float
        val lastIndex = words.lastIndex
        val desiredWidth = getLineWidth(line)
        if (lineCompressMod[line] > 0) {
            val gapCount: Int = lastIndex - 1
            val lastWordsWith = getDesiredWidth(words[lastIndex], curPaint)
            total = visibleWidth - desiredWidth + lastWordsWith
            d = total / gapCount
        } else {
            val gapCount: Int = lastIndex
            total = visibleWidth - desiredWidth
            d = total / gapCount
        }
        interval.total = total
        interval.single = d
        return interval
    }

    /*
    * @fun：获取当前行不同字符的位置
    * @in：行，当前字符对于最后一个字符的偏移值，字符，间隔，定位参数
    * @out：定位参数
    */
    fun getLocate(line: Int, idx: Int, string: String, interval: Float, locate: Locate) {
        val cw = getDesiredWidth(string, curPaint)
        when (lineCompressMod[line]) {
            1 -> {
                when (idx) {
                    1 -> {
                        val offset = getPostPancOffset(string)
                        locate.start -= offset
                        locate.end = locate.start + cw / 2 + offset
                    }
                    0 -> {
                        locate.start -= getPostPancOffset(string)
                        locate.end = locate.start + cw
                    }
                    else -> {
                        locate.end = locate.start + cw + interval
                    }
                }
            }
            2 -> {
                when (idx) {
                    2 -> {
                        val offset = getPostPancOffset(string)
                        locate.start -= offset
                        locate.end = locate.start + cw / 2 + offset
                    }
                    1 -> {
                        val offset = getPostPancOffset(string)
                        locate.start -= offset
                        locate.end = locate.start + cw / 2 + offset
                    }
                    0 -> {
                        locate.end = locate.start + cw
                    }
                    else -> {
                        locate.end = locate.start + cw + interval
                    }
                }
            }
            3 -> {
                when (idx) {
                    2 -> {
                        val offset = getPrePancOffset(string)
                        locate.start -= offset
                        locate.end = locate.start + cw / 2 + offset
                    }
                    1 -> {
                        locate.end = locate.start + cw + interval
                    }
                    0 -> {
                        locate.start -= getPostPancOffset(string)
                        locate.end = locate.start + cw
                    }
                    else -> {
                        locate.end = locate.start + cw + interval
                    }
                }
            }
            else -> {
                locate.end = if (idx != 0) (locate.start + cw + interval) else (locate.start + cw)
            }
        }
    }

}