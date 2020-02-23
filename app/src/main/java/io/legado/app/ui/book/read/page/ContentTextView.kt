package io.legado.app.ui.book.read.page

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
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
        ReadBookConfig.let {
            ChapterProvider.viewWidth = w
            ChapterProvider.viewHeight = h
            ChapterProvider.upSize()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (textPage.textLines.isEmpty()) {
            drawMsg(canvas, textPage.text)
        } else {
            drawHorizontalPage(canvas)
        }
    }

    @Suppress("DEPRECATION")
    private fun drawMsg(canvas: Canvas, msg: String) {
        val layout = StaticLayout(
            msg, ChapterProvider.contentPaint, width,
            Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
        )
        val y = (height - layout.height) / 2f
        for (lineIndex in 0 until layout.lineCount) {
            val x = (width - layout.getLineMax(lineIndex)) / 2
            val words =
                msg.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
            canvas.drawText(words, x, y, ChapterProvider.contentPaint)
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
                    it.leftBottomPosition.x,
                    it.leftBottomPosition.y,
                    textPaint
                )
                if (it.selected) {
                    canvas.drawRect(
                        it.leftBottomPosition.x,
                        textLine.lineTop,
                        it.rightTopPosition.x,
                        textLine.lineBottom,
                        selectedPaint
                    )
                }
            }
        }
    }

    fun onScroll(mOffset: Float) {
        var offset = mOffset
        if (offset > maxScrollOffset) {
            offset = maxScrollOffset
        } else if (offset < -maxScrollOffset) {
            offset = -maxScrollOffset
        }

        if (!isLastPage || offset < 0) {
            pageOffset += offset
            isLastPage = false
        }
        // 首页
        if (pageOffset < 0 && !pageFactory.hasPrev()) {
            pageOffset = 0f
        }

        val cHeight = if (textPage.height > 0) textPage.height else height
        if (offset > 0 && pageOffset > cHeight) {

        }
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
                    if (x > textChar.leftBottomPosition.x && x < textChar.rightTopPosition.x) {
                        textChar.selected = true
                        invalidate()
                        selectLineStart = lineIndex
                        selectCharStart = charIndex
                        selectLineEnd = lineIndex
                        selectCharEnd = charIndex
                        upSelectedStart(
                            textChar.leftBottomPosition.x,
                            textChar.leftBottomPosition.y
                        )
                        upSelectedEnd(
                            textChar.rightTopPosition.x,
                            textChar.leftBottomPosition.y
                        )
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
                    if (x > textChar.leftBottomPosition.x && x < textChar.rightTopPosition.x) {
                        if (selectLineStart != lineIndex || selectCharStart != charIndex) {
                            selectLineStart = lineIndex
                            selectCharStart = charIndex
                            upSelectedStart(
                                textChar.leftBottomPosition.x,
                                textChar.leftBottomPosition.y
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

    fun selectEndMove(x: Float, y: Float) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop && y < textLine.lineBottom) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.leftBottomPosition.x && x < textChar.rightTopPosition.x) {
                        if (selectLineEnd != lineIndex || selectCharEnd != charIndex) {
                            selectLineEnd = lineIndex
                            selectCharEnd = charIndex
                            upSelectedEnd(
                                textChar.rightTopPosition.x,
                                textChar.leftBottomPosition.y
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
