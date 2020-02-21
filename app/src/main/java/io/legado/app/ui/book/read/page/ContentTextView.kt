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
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean


class ContentTextView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble)
    private val selectedPaint by lazy {
        Paint().apply {
            color = context.getCompatColor(R.color.btn_bg_press)
        }
    }
    var textPage: TextPage? = null

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

}
