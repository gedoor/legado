package io.legado.app.ui.book.read.page

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
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
    private var selectLineStart = 0
    private var selectCharStart = 0
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
        textPage.format()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipRect(
            ChapterProvider.paddingLeft,
            ChapterProvider.paddingTop,
            ChapterProvider.visibleRight,
            ChapterProvider.visibleBottom
        )
        drawPage(canvas)
    }

    private fun drawPage(canvas: Canvas) {
        val mPageOffset = pageOffset
        textPage.textLines.forEach { textLine ->
            val lineTop = textLine.lineTop + mPageOffset
            val lineBase = textLine.lineBase + mPageOffset
            val lineBottom = textLine.lineBottom + mPageOffset
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
        if (!ReadBookConfig.isScroll) {
            return
        }
        val nextPage = pageFactory.nextPage
        nextPage.textLines.forEach { textLine ->
            val yPy = mPageOffset + textPage.height
            val lineTop = textLine.lineTop + yPy
            val lineBase = textLine.lineBase + yPy
            val lineBottom = textLine.lineBottom + yPy
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
        if (mPageOffset + textPage.height + nextPage.height < ChapterProvider.visibleHeight) {
            pageFactory.nextPagePlus.textLines.forEach { textLine ->
                val yPy = mPageOffset + textPage.height + nextPage.height
                val lineTop = textLine.lineTop + yPy
                val lineBase = textLine.lineBase + yPy
                val lineBottom = textLine.lineBottom + yPy
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

    fun selectText(x: Float, y: Float, select: (lineIndex: Int, charIndex: Int) -> Unit) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop + pageOffset && y < textLine.lineBottom + pageOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        textChar.selected = true
                        invalidate()
                        selectLineStart = lineIndex
                        selectCharStart = charIndex
                        selectLineEnd = lineIndex
                        selectCharEnd = charIndex
                        upSelectedStart(textChar.start, textLine.lineBottom + pageOffset)
                        upSelectedEnd(textChar.end, textLine.lineBottom + pageOffset)
                        select(lineIndex, charIndex)
                    }
                }
                break
            }
        }
    }

    fun selectStartMove(x: Float, y: Float) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop + pageOffset && y < textLine.lineBottom + pageOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineStart != lineIndex || selectCharStart != charIndex) {
                            selectLineStart = lineIndex
                            selectCharStart = charIndex
                            upSelectedStart(textChar.start, textLine.lineBottom)
                            upSelectChars(textPage)
                        }
                        break
                    }
                }
                break
            }
        }
    }

    fun selectStartMoveIndex(lineIndex: Int, charIndex: Int) {
        selectLineStart = lineIndex
        selectCharStart = charIndex
        val textLine = textPage.textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedStart(textChar.start, textLine.lineBottom + pageOffset)
        upSelectChars(textPage)
    }

    fun selectEndMove(x: Float, y: Float) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop + pageOffset && y < textLine.lineBottom + pageOffset) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineEnd != lineIndex || selectCharEnd != charIndex) {
                            selectLineEnd = lineIndex
                            selectCharEnd = charIndex
                            upSelectedEnd(textChar.end, textLine.lineBottom + pageOffset)
                            upSelectChars(textPage)
                        }
                        break
                    }
                }
                break
            }
        }
    }

    fun selectEndMoveIndex(lineIndex: Int, charIndex: Int) {
        selectLineEnd = lineIndex
        selectCharEnd = charIndex
        val textLine = textPage.textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedEnd(textChar.end, textLine.lineBottom + pageOffset)
        upSelectChars(textPage)
    }

    private fun upSelectChars(textPage: TextPage) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                textChar.selected =
                    if (lineIndex == selectLineStart && lineIndex == selectLineEnd) {
                        charIndex in selectCharStart..selectCharEnd
                    } else if (lineIndex == selectLineStart) {
                        charIndex >= selectCharStart
                    } else if (lineIndex == selectLineEnd) {
                        charIndex <= selectCharEnd
                    } else {
                        lineIndex in (selectLineStart + 1) until selectLineEnd
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
        textPage.textLines.forEach { textLine ->
            textLine.textChars.forEach {
                it.selected = false
            }
        }
        invalidate()
        callBack.onCancelSelect()
    }

    val selectedText: String
        get() {
            val stringBuilder = StringBuilder()
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
            return stringBuilder.toString()
        }

    interface CallBack {
        fun upSelectedStart(x: Float, y: Float)
        fun upSelectedEnd(x: Float, y: Float)
        fun onCancelSelect()
        val headerHeight: Int
        val pageFactory: TextPageFactory
    }
}
