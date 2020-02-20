package io.legado.app.ui.book.read.page

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.page.entities.TextPage


class ContentTextView : View {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

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
                textLine.textChars.forEach {
                    if (textLine.isTitle) {
                        canvas.drawText(
                            it.charData,
                            it.leftBottomPosition.x.toFloat(),
                            it.leftBottomPosition.y.toFloat(),
                            ChapterProvider.titlePaint
                        )
                    } else {
                        canvas.drawText(
                            it.charData,
                            it.leftBottomPosition.x.toFloat(),
                            it.leftBottomPosition.y.toFloat(),
                            ChapterProvider.contentPaint
                        )
                    }
                }
            }
        }
    }

}
