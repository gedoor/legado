package io.legado.app.ui.widget.text

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MultilineTextView(context: Context?, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    private var calculatedLines = false

    override fun onDraw(canvas: Canvas?) {
        if (!calculatedLines) {
            calculateLines();
            calculatedLines = true;
        }
        super.onDraw(canvas)
    }

    private fun calculateLines() {
        val mHeight = measuredHeight
        val lHeight = lineHeight
        val lines = mHeight / lHeight
        setLines(lines)
    }
}