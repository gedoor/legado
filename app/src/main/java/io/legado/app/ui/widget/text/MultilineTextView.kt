package io.legado.app.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MultilineTextView(context: Context, attrs: AttributeSet?) :
    AppCompatTextView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        calculateLines(heightSize)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun calculateLines(measuredHeight: Int) {
        val lHeight = lineHeight
        val lines = measuredHeight / lHeight
        setLines(lines)
    }
}