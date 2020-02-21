package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.read.page.entities.SelectPoint
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean


class ContentTextView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val selectedPaint by lazy {
        Paint().apply {
            color = context.getCompatColor(R.color.btn_bg_press_2)
            style = Paint.Style.FILL
        }
    }
    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble)
    var selectStartLine = 0
    var selectStartChar = 0
    var selectEndLine = 0
    var selectEndChar = 0
    private var textPage: TextPage? = null

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return true
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

    fun selectText(x: Float, y: Float): SelectPoint? {
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
                            return SelectPoint(
                                textChar.leftBottomPosition.x,
                                textChar.leftBottomPosition.y.toFloat(),
                                textChar.rightTopPosition.x,
                                textChar.leftBottomPosition.y.toFloat()
                            )
                        }
                    }
                    break
                }
            }
        }
        return null
    }

    fun selectStartMove(x: Float, y: Float) {

    }

    fun selectEndMove(x: Float, y: Float) {

    }

}
