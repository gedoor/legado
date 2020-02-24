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
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.activity
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean


class ContentTextView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val selectedPaint by lazy {
        Paint().apply {
            color = context.getCompatColor(R.color.btn_bg_press_2)
            style = Paint.Style.FILL
        }
    }
    private var callBack: CallBack
    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble)
    private var selectLineStart = 0
    private var selectCharStart = 0
    private var selectLineEnd = 0
    private var selectCharEnd = 0
    private var textPage: TextPage = TextPage()
    //滚动参数
    private val pageFactory: TextPageFactory get() = callBack.pageFactory
    private val maxScrollOffset = 100f
    private var pageOffset = 0f
    private var linePos = 0
    private var isLastPage = false

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
        if (ReadBookConfig.isScroll) {
            drawScrollPage(canvas)
        } else {
            drawHorizontalPage(canvas)
        }
    }

    private fun drawHorizontalPage(canvas: Canvas) {
        textPage.textLines.forEach { textLine ->
            val textPaint = if (textLine.isTitle) {
                ChapterProvider.titlePaint
            } else {
                ChapterProvider.contentPaint
            }
            textPaint.color = if (textLine.isReadAloud) {
                context.accentColor
            } else {
                ReadBookConfig.durConfig.textColor()
            }
            textLine.textChars.forEach {
                canvas.drawText(
                    it.charData,
                    it.start,
                    textLine.lineBase,
                    textPaint
                )
                if (it.selected) {
                    canvas.drawRect(
                        it.start,
                        textLine.lineTop,
                        it.end,
                        textLine.lineBottom,
                        selectedPaint
                    )
                }
            }
        }
    }

    private fun drawScrollPage(canvas: Canvas) {
        if (pageOffset <= 0) {
            textPage.textLines.forEach { textLine ->
                val textPaint = if (textLine.isTitle) {
                    ChapterProvider.titlePaint
                } else {
                    ChapterProvider.contentPaint
                }
                textPaint.color = if (textLine.isReadAloud) {
                    context.accentColor
                } else {
                    ReadBookConfig.durConfig.textColor()
                }
                textLine.textChars.forEach {
                    canvas.drawText(
                        it.charData,
                        it.start,
                        textLine.lineBase + pageOffset,
                        textPaint
                    )
                    if (it.selected) {
                        canvas.drawRect(
                            it.start,
                            textLine.lineTop + pageOffset,
                            it.end,
                            textLine.lineBottom + pageOffset,
                            selectedPaint
                        )
                    }
                }
            }
            pageFactory.nextPage?.textLines?.forEach { textLine ->
                val textPaint = if (textLine.isTitle) {
                    ChapterProvider.titlePaint
                } else {
                    ChapterProvider.contentPaint
                }
                textPaint.color = if (textLine.isReadAloud) {
                    context.accentColor
                } else {
                    ReadBookConfig.durConfig.textColor()
                }
                textLine.textChars.forEach {
                    canvas.drawText(
                        it.charData,
                        it.start,
                        textLine.lineBase + pageOffset + textPage.height - ChapterProvider.paddingTop,
                        textPaint
                    )
                    if (it.selected) {
                        canvas.drawRect(
                            it.start,
                            textLine.lineTop + pageOffset + textPage.height - ChapterProvider.paddingTop,
                            it.end,
                            textLine.lineBottom + pageOffset + textPage.height - ChapterProvider.paddingTop,
                            selectedPaint
                        )
                    }
                }
            }
        } else {
            textPage.textLines.forEach { textLine ->
                val textPaint = if (textLine.isTitle) {
                    ChapterProvider.titlePaint
                } else {
                    ChapterProvider.contentPaint
                }
                textPaint.color = if (textLine.isReadAloud) {
                    context.accentColor
                } else {
                    ReadBookConfig.durConfig.textColor()
                }
                textLine.textChars.forEach {
                    canvas.drawText(
                        it.charData,
                        it.start,
                        textLine.lineBase + pageOffset,
                        textPaint
                    )
                    if (it.selected) {
                        canvas.drawRect(
                            it.start,
                            textLine.lineTop + pageOffset,
                            it.end,
                            textLine.lineBottom + pageOffset,
                            selectedPaint
                        )
                    }
                }
            }
        }
    }

    fun onScroll(mOffset: Float) {
        if (mOffset == 0f) return
        var offset = -mOffset
        if (offset > maxScrollOffset) {
            offset = maxScrollOffset
        } else if (offset < -maxScrollOffset) {
            offset = -maxScrollOffset
        }

        pageOffset += offset
        invalidate()
    }

    fun resetPageOffset() {
        pageOffset = 0f
        linePos = 0
        isLastPage = false
    }

    private fun switchToPageOffset(offset: Int) {
        when (offset) {
            1 -> {

            }
            -1 -> {

            }
        }
    }

    fun selectText(x: Float, y: Float): Boolean {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop && y < textLine.lineBottom) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        textChar.selected = true
                        invalidate()
                        selectLineStart = lineIndex
                        selectCharStart = charIndex
                        selectLineEnd = lineIndex
                        selectCharEnd = charIndex
                        upSelectedStart(textChar.start, textLine.lineBottom)
                        upSelectedEnd(textChar.end, textLine.lineBottom)
                        return true
                    }
                }
                break
            }
        }
        return false
    }

    fun selectStartMove(x: Float, y: Float) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop && y < textLine.lineBottom) {
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

    fun selectEndMove(x: Float, y: Float) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop && y < textLine.lineBottom) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineEnd != lineIndex || selectCharEnd != charIndex) {
                            selectLineEnd = lineIndex
                            selectCharEnd = charIndex
                            upSelectedEnd(
                                textChar.end,
                                textLine.lineBottom
                            )
                            upSelectChars(textPage)
                        }
                        break
                    }
                }
                break
            }
        }
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
