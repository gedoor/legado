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
    private var activityCallBack: CallBack
    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble)
    var selectStartLine = 0
    var selectStartChar = 0
    var selectEndLine = 0
    var selectEndChar = 0
    private var textPage: TextPage? = null

    init {
        activityCallBack = activity as CallBack
    }

    fun setContent(textPage: TextPage?) {
        this.textPage = textPage
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ReadBookConfig.durConfig.let {
            ChapterProvider.viewWidth = w
            ChapterProvider.viewHeight = h
            ChapterProvider.upSize(ReadBookConfig.durConfig)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        textPage?.let { textPage ->
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
                        it.leftBottomPosition.y.toFloat(),
                        textPaint
                    )
                    if (it.selected) {
                        canvas.drawRect(
                            it.leftBottomPosition.x,
                            it.rightTopPosition.y.toFloat(),
                            it.rightTopPosition.x,
                            it.leftBottomPosition.y.toFloat(),
                            selectedPaint
                        )
                    }
                }
            }
        }
    }

    fun selectText(x: Float, y: Float): Boolean {
        textPage?.let { textPage ->
            for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
                if (y > textLine.lineTop && y < textLine.lineBottom) {
                    for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                        if (x > textChar.leftBottomPosition.x && x < textChar.rightTopPosition.x) {
                            textChar.selected = true
                            invalidate()
                            selectStartLine = lineIndex
                            selectStartChar = charIndex
                            selectEndLine = lineIndex
                            selectEndChar = charIndex
                            upSelectedStart(
                                textChar.leftBottomPosition.x,
                                textChar.leftBottomPosition.y.toFloat()
                            )
                            upSelectedEnd(
                                textChar.rightTopPosition.x,
                                textChar.leftBottomPosition.y.toFloat()
                            )
                            return true
                        }
                    }
                    break
                }
            }
        }
        return false
    }

    fun selectStartMove(x: Float, y: Float) {
        textPage?.let { textPage ->
            for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
                if (y > textLine.lineTop && y < textLine.lineBottom) {
                    for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                        if (x > textChar.leftBottomPosition.x && x < textChar.rightTopPosition.x) {
                            if (selectStartLine != lineIndex || selectStartChar != charIndex) {
                                selectStartLine = lineIndex
                                selectStartChar = charIndex
                                upSelectedStart(
                                    textChar.leftBottomPosition.x,
                                    textChar.leftBottomPosition.y.toFloat()
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
    }

    fun selectEndMove(x: Float, y: Float) {
        textPage?.let { textPage ->
            for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
                if (y > textLine.lineTop && y < textLine.lineBottom) {
                    for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                        if (x > textChar.leftBottomPosition.x && x < textChar.rightTopPosition.x) {
                            if (selectEndLine != lineIndex || selectEndChar != charIndex) {
                                selectEndLine = lineIndex
                                selectEndChar = charIndex
                                upSelectedEnd(
                                    textChar.rightTopPosition.x,
                                    textChar.leftBottomPosition.y.toFloat()
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
    }

    private fun upSelectChars(textPage: TextPage) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                textChar.selected =
                    if (lineIndex == selectStartLine && lineIndex == selectEndLine) {
                        charIndex in selectStartChar..selectEndChar
                    } else if (lineIndex == selectStartLine) {
                        charIndex >= selectStartChar
                    } else if (lineIndex == selectEndLine) {
                        charIndex <= selectEndChar
                    } else {
                        lineIndex in (selectStartLine + 1) until selectEndLine
                    }
            }
        }
        invalidate()
    }

    private fun upSelectedStart(x: Float, y: Float) {
        activityCallBack.upSelectedStart(x, y + activityCallBack.headerHeight)
    }

    private fun upSelectedEnd(x: Float, y: Float) {
        activityCallBack.upSelectedEnd(x, y + activityCallBack.headerHeight)
    }

    fun cancelSelect() {
        textPage?.let { textPage ->
            textPage.textLines.forEach { textLine ->
                textLine.textChars.forEach {
                    it.selected = false
                }
            }
            invalidate()
        }
        activityCallBack.onCancelSelect()
    }

    val selectedText: String
        get() {
            return ""
        }

    interface CallBack {
        fun upSelectedStart(x: Float, y: Float)
        fun upSelectedEnd(x: Float, y: Float)
        fun onCancelSelect()
        val headerHeight: Int
    }
}
