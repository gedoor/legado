package io.legado.app.ui.book.source.manage

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.dpToPx
import io.legado.app.utils.spToPx
import splitties.init.appCtx

class BookSourceDecoration(val adapter: BookSourceAdapter): RecyclerView.ItemDecoration() {

    private val headerLeft = 16f.dpToPx()
    private val headerHeight = 32f.dpToPx()

    private val headerPaint = Paint().apply {
        color = appCtx.backgroundColor
    }
    private val textPaint = TextPaint().apply {
        textSize = 16f.spToPx()
        color = appCtx.accentColor
        isAntiAlias = true
    }
    private val textRect = Rect()

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val count = parent.childCount
        for (i in 0 until count) {
            val view = parent.getChildAt(i)
            val position = parent.getChildLayoutPosition(view)
            val isHeader = adapter.isItemHeader(position)
            if (isHeader) {
                c.drawRect(
                    0f,
                    view.top - headerHeight,
                    parent.width.toFloat(),
                    view.top.toFloat(),
                    headerPaint
                )
                val headerText = adapter.getHeaderText(position)
                textPaint.getTextBounds(headerText, 0, headerText.length, textRect)
                c.drawText(
                    headerText,
                    headerLeft,
                    (view.top - headerHeight) + headerHeight / 2 + textRect.height() / 2,
                    textPaint
                )
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildLayoutPosition(view)
        val isHeader = adapter.isItemHeader(position)
        if (isHeader) {
            outRect.top = headerHeight.toInt()
        }
    }

}
