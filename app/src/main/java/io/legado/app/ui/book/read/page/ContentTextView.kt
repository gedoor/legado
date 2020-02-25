package io.legado.app.ui.book.read.page

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.read.page.entities.TextChar
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.activity
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean


class ContentTextView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble)
    var upView: ((TextPage) -> Unit)? = null
    private val selectedPaint by lazy {
        Paint().apply {
            color = context.getCompatColor(R.color.btn_bg_press_2)
            style = Paint.Style.FILL
        }
    }
    private var callBack: CallBack
    private val visibleRect = RectF()
    private var selectPageStart = 0
    private var selectLineStart = 0
    private var selectCharStart = 0
    private var selectPageEnd = 0
    private var selectLineEnd = 0
    private var selectCharEnd = 0
    private var textPage: TextPage = TextPage()
    //滚动参数
    private val pageFactory: TextPageFactory get() = callBack.pageFactory
    private val maxScrollOffset = 100f
    private var pageOffset = 0f

    init {
        callBack = activity as CallBack
        contentDescription = textPage.text
    }

    fun setContent(textPage: TextPage) {
        this.textPage = textPage
        contentDescription = textPage.text
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ChapterProvider.viewWidth = w
        ChapterProvider.viewHeight = h
        ChapterProvider.upSize()
        visibleRect.set(
            ChapterProvider.paddingLeft.toFloat(),
            ChapterProvider.paddingTop.toFloat(),
            ChapterProvider.visibleRight.toFloat(),
            ChapterProvider.visibleBottom.toFloat()
        )
        textPage.format()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipRect(visibleRect)
        drawPage(canvas)
    }

    /**
     * 绘制页面
     */
    private fun drawPage(canvas: Canvas) {
        var relativeOffset = relativeOffset(0)
        textPage.textLines.forEach { textLine ->
            val lineTop = textLine.lineTop + relativeOffset
            val lineBase = textLine.lineBase + relativeOffset
            val lineBottom = textLine.lineBottom + relativeOffset
            drawChars(
                canvas,
                textLine.textChars,
                lineTop,
                lineBase,
                lineBottom,
                textLine.isTitle,
                textLine.isReadAloud
            )
        }
        if (!ReadBookConfig.isScroll) return
        //滚动翻页
        val nextPage = relativePage(1)
        relativeOffset = relativeOffset(1)
        nextPage.textLines.forEach { textLine ->
            val lineTop = textLine.lineTop + relativeOffset
            val lineBase = textLine.lineBase + relativeOffset
            val lineBottom = textLine.lineBottom + relativeOffset
            drawChars(
                canvas,
                textLine.textChars,
                lineTop,
                lineBase,
                lineBottom,
                textLine.isTitle,
                textLine.isReadAloud
            )
        }
        relativeOffset = relativeOffset(2)
        if (relativeOffset < ChapterProvider.visibleHeight) {
            relativePage(2).textLines.forEach { textLine ->
                val lineTop = textLine.lineTop + relativeOffset
                val lineBase = textLine.lineBase + relativeOffset
                val lineBottom = textLine.lineBottom + relativeOffset
                drawChars(
                    canvas,
                    textLine.textChars,
                    lineTop,
                    lineBase,
                    lineBottom,
                    textLine.isTitle,
                    textLine.isReadAloud
                )
            }
        }
    }

    /**
     * 绘制文字
     */
    private fun drawChars(
        canvas: Canvas,
        textChars: List<TextChar>,
        lineTop: Float,
        lineBase: Float,
        lineBottom: Float,
        isTitle: Boolean,
        isReadAloud: Boolean
    ) {
        val textPaint = if (isTitle) ChapterProvider.titlePaint else ChapterProvider.contentPaint
        textPaint.color =
            if (isReadAloud) context.accentColor else ReadBookConfig.durConfig.textColor()
        textChars.forEach {
            canvas.drawText(it.charData, it.start, lineBase, textPaint)
            if (it.selected) {
                canvas.drawRect(it.start, lineTop, it.end, lineBottom, selectedPaint)
            }
        }
    }

    /**
     * 滚动事件
     */
    fun onScroll(mOffset: Float) {
        if (mOffset == 0f) return
        var offset = mOffset
        if (offset > maxScrollOffset) {
            offset = maxScrollOffset
        } else if (offset < -maxScrollOffset) {
            offset = -maxScrollOffset
        }

        pageOffset += offset
        if (pageOffset > 0) {
            pageFactory.moveToPrev()
            textPage = pageFactory.currentPage
            pageOffset -= textPage.height
            upView?.invoke(textPage)
        } else if (pageOffset < -textPage.height) {
            pageOffset += textPage.height
            pageFactory.moveToNext()
            textPage = pageFactory.currentPage
            upView?.invoke(textPage)
        }
        invalidate()
    }

    fun resetPageOffset() {
        pageOffset = 0f
    }

    /**
     * 选择初始文字
     */
    fun selectText(
        x: Float,
        y: Float,
        select: (relativePage: Int, lineIndex: Int, charIndex: Int) -> Unit
    ) {
        if (!visibleRect.contains(x, y)) return
        var relativeOffset = relativeOffset(0)
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        textChar.selected = true
                        invalidate()
                        selectPageStart = 0
                        selectLineStart = lineIndex
                        selectCharStart = charIndex
                        selectPageEnd = 0
                        selectLineEnd = lineIndex
                        selectCharEnd = charIndex
                        upSelectedStart(textChar.start, textLine.lineBottom + relativeOffset)
                        upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset)
                        select(0, lineIndex, charIndex)
                        return
                    }
                }
                return
            }
        }
        if (!ReadBookConfig.isScroll) return
        //滚动翻页
        relativeOffset = relativeOffset(1)
        if (relativeOffset >= ChapterProvider.visibleHeight) return
        val nextPage = relativePage(1)
        for ((lineIndex, textLine) in nextPage.textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        textChar.selected = true
                        invalidate()
                        selectPageStart = 1
                        selectLineStart = lineIndex
                        selectCharStart = charIndex
                        selectPageEnd = 1
                        selectLineEnd = lineIndex
                        selectCharEnd = charIndex
                        upSelectedStart(textChar.start, textLine.lineBottom + relativeOffset)
                        upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset)
                        select(1, lineIndex, charIndex)
                        return
                    }
                }
                return
            }
        }
        relativeOffset = relativeOffset(2)
        if (relativeOffset >= ChapterProvider.visibleHeight) return
        for ((lineIndex, textLine) in relativePage(2).textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        textChar.selected = true
                        invalidate()
                        selectPageStart = 2
                        selectLineStart = lineIndex
                        selectCharStart = charIndex
                        selectPageEnd = 2
                        selectLineEnd = lineIndex
                        selectCharEnd = charIndex
                        upSelectedStart(textChar.start, textLine.lineBottom + relativeOffset)
                        upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset)
                        select(2, lineIndex, charIndex)
                        return
                    }
                }
                return
            }
        }
    }

    /**
     * 开始选择符移动
     */
    fun selectStartMove(x: Float, y: Float) {
        if (!visibleRect.contains(x, y)) return
        var relativeOffset = relativeOffset(0)
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineStart != lineIndex || selectCharStart != charIndex) {
                            selectPageStart = 0
                            selectLineStart = lineIndex
                            selectCharStart = charIndex
                            upSelectedStart(textChar.start, textLine.lineBottom + relativeOffset)
                            upSelectChars()
                        }
                        return
                    }
                }
                return
            }
        }
        if (!ReadBookConfig.isScroll) return
        //滚动翻页
        relativeOffset = relativeOffset(1)
        if (relativeOffset >= ChapterProvider.visibleHeight) return
        for ((lineIndex, textLine) in relativePage(1).textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineStart != lineIndex || selectCharStart != charIndex) {
                            selectPageStart = 1
                            selectLineStart = lineIndex
                            selectCharStart = charIndex
                            upSelectedStart(textChar.start, textLine.lineBottom + relativeOffset)
                            upSelectChars()
                        }
                        return
                    }
                }
                return
            }
        }
        relativeOffset = relativeOffset(2)
        if (relativeOffset >= ChapterProvider.visibleHeight) return
        for ((lineIndex, textLine) in relativePage(2).textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineStart != lineIndex || selectCharStart != charIndex) {
                            selectPageStart = 1
                            selectLineStart = lineIndex
                            selectCharStart = charIndex
                            upSelectedStart(textChar.start, textLine.lineBottom + relativeOffset)
                            upSelectChars()
                        }
                        return
                    }
                }
                return
            }
        }
    }

    /**
     * 结束选择符移动
     */
    fun selectEndMove(x: Float, y: Float) {
        if (!visibleRect.contains(x, y)) return
        var relativeOffset = relativeOffset(0)
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineEnd != lineIndex || selectCharEnd != charIndex) {
                            selectLineEnd = lineIndex
                            selectCharEnd = charIndex
                            upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset)
                            upSelectChars()
                        }
                        return
                    }
                }
                return
            }
        }
        if (!ReadBookConfig.isScroll) return
        //滚动翻页
        relativeOffset = relativeOffset(1)
        if (relativeOffset >= ChapterProvider.visibleHeight) return
        for ((lineIndex, textLine) in relativePage(1).textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineEnd != lineIndex || selectCharEnd != charIndex) {
                            selectLineEnd = lineIndex
                            selectCharEnd = charIndex
                            upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset)
                            upSelectChars()
                        }
                        return
                    }
                }
                return
            }
        }
        relativeOffset = relativeOffset(2)
        if (relativeOffset >= ChapterProvider.visibleHeight) return
        for ((lineIndex, textLine) in relativePage(2).textLines.withIndex()) {
            if (y > textLine.lineTop + relativeOffset && y < textLine.lineBottom + relativeOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineEnd != lineIndex || selectCharEnd != charIndex) {
                            selectLineEnd = lineIndex
                            selectCharEnd = charIndex
                            upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset)
                            upSelectChars()
                        }
                        return
                    }
                }
                return
            }
        }
    }

    /**
     * 选择开始文字
     */
    fun selectStartMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        selectPageStart = relativePage
        selectLineStart = lineIndex
        selectCharStart = charIndex
        val textLine = relativePage(relativePage).textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedStart(textChar.start, textLine.lineBottom + relativeOffset(relativePage))
        upSelectChars()
    }

    /**
     * 选择结束文字
     */
    fun selectEndMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        selectPageEnd = relativePage
        selectLineEnd = lineIndex
        selectCharEnd = charIndex
        val textLine = relativePage(relativePage).textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedEnd(textChar.end, textLine.lineBottom + relativeOffset(relativePage))
        upSelectChars()
    }

    private fun upSelectChars() {
        val last = if (ReadBookConfig.isScroll) 2 else 0
        for (relativePos in 0..last) {
            for ((lineIndex, textLine) in relativePage(relativePos).textLines.withIndex()) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    textChar.selected =
                        if (relativePos == selectPageStart
                            && relativePos == selectPageEnd
                            && lineIndex == selectLineStart
                            && lineIndex == selectLineEnd
                        ) {
                            charIndex in selectCharStart..selectCharEnd
                        } else if (relativePos == selectPageStart && lineIndex == selectLineStart) {
                            charIndex >= selectCharStart
                        } else if (relativePos == selectPageEnd && lineIndex == selectLineEnd) {
                            charIndex <= selectCharEnd
                        } else if (relativePos == selectPageStart && relativePos == selectPageEnd) {
                            lineIndex in (selectLineStart + 1) until selectLineEnd
                        } else if (relativePos == selectPageStart) {
                            lineIndex > selectLineStart
                        } else if (relativePos == selectPageEnd) {
                            lineIndex < selectLineEnd
                        } else {
                            relativePos in selectPageStart + 1 until selectPageEnd
                        }
                }
            }
        }
        invalidate()
    }

    private fun upSelectedStart(x: Float, y: Float) {
        callBack.upSelectedStart(x, y + callBack.headerHeight)
    }

    private fun upSelectedEnd(x: Float, y: Float) {
        callBack.upSelectedEnd(x, y + callBack.headerHeight)
    }

    fun cancelSelect() {
        val last = if (ReadBookConfig.isScroll) 2 else 0
        for (relativePos in 0..last) {
            relativePage(relativePos).textLines.forEach { textLine ->
                textLine.textChars.forEach {
                    it.selected = false
                }
            }
        }
        invalidate()
        callBack.onCancelSelect()
    }

    val selectedText: String
        get() {
            val stringBuilder = StringBuilder()
            for (relativePos in selectPageStart..selectPageEnd) {
                val textPage = relativePage(relativePos)
                if (relativePos == selectPageStart && relativePos == selectPageEnd) {
                    for (lineIndex in selectLineStart..selectLineEnd) {
                        if (lineIndex == selectLineStart && lineIndex == selectLineEnd) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(
                                    selectCharStart,
                                    selectCharEnd + 1
                                )
                            )
                        } else if (lineIndex == selectLineStart) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(
                                    selectCharStart
                                )
                            )
                        } else if (lineIndex == selectLineEnd) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(0, selectCharEnd + 1)
                            )
                        } else {
                            stringBuilder.append(textPage.textLines[lineIndex].text)
                        }
                    }
                } else if (relativePos == selectPageStart) {
                    for (lineIndex in selectLineStart until relativePage(relativePos).textLines.size) {
                        if (lineIndex == selectLineStart) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(
                                    selectCharStart
                                )
                            )
                        } else {
                            stringBuilder.append(textPage.textLines[lineIndex].text)
                        }
                    }
                } else if (relativePos == selectPageEnd) {
                    for (lineIndex in 0..selectLineEnd) {
                        if (lineIndex == selectLineEnd) {
                            stringBuilder.append(
                                textPage.textLines[lineIndex].text.substring(0, selectCharEnd + 1)
                            )
                        } else {
                            stringBuilder.append(textPage.textLines[lineIndex].text)
                        }
                    }
                } else if (relativePos in selectPageStart + 1 until selectPageEnd) {
                    for (lineIndex in selectLineStart..selectLineEnd) {
                        stringBuilder.append(textPage.textLines[lineIndex].text)
                    }
                }
            }
            return stringBuilder.toString()
        }

    private fun relativeOffset(relativePos: Int): Float {
        return when (relativePos) {
            0 -> pageOffset
            1 -> pageOffset + textPage.height
            else -> pageOffset + textPage.height + pageFactory.nextPage.height
        }
    }

    private fun relativePage(relativePos: Int): TextPage {
        return when (relativePos) {
            0 -> textPage
            1 -> pageFactory.nextPage
            else -> pageFactory.nextPagePlus
        }
    }

    interface CallBack {
        fun upSelectedStart(x: Float, y: Float)
        fun upSelectedEnd(x: Float, y: Float)
        fun onCancelSelect()
        val headerHeight: Int
        val pageFactory: TextPageFactory
    }
}
