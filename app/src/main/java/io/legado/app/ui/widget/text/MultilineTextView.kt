package io.legado.app.ui.widget.text

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MultilineTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    override fun onDraw(canvas: Canvas) {
        calculateLines()
        super.onDraw(canvas)
    }

    private fun calculateLines() {
        val mHeight = measuredHeight
        val lHeight = lineHeight
        val lines = mHeight / lHeight
        setLines(lines)
    }
}