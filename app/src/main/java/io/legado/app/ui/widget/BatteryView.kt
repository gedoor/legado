package io.legado.app.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.utils.dp

class BatteryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    private val batteryTypeface by lazy {
        Typeface.createFromAsset(context.assets, "font/number.ttf")
    }
    private val batteryPaint = Paint()
    private val outFrame = Rect()
    private val polar = Rect()
    var isBattery = false
        set(value) {
            field = value
            if (value) {
                super.setTypeface(batteryTypeface)
                postInvalidate()
            }
        }

    init {
        setPadding(4.dp, 2.dp, 6.dp, 2.dp)
        batteryPaint.strokeWidth = 1.dp.toFloat()
        batteryPaint.isAntiAlias = true
        batteryPaint.color = paint.color
    }

    override fun setTypeface(tf: Typeface?) {
        if (!isBattery) {
            super.setTypeface(tf)
        }
    }

    fun setColor(@ColorInt color: Int) {
        setTextColor(color)
        batteryPaint.color = color
        invalidate()
    }

    @SuppressLint("SetTextI18n")
    fun setBattery(battery: Int) {
        text = "$battery"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isBattery) return
        outFrame.set(
            1.dp,
            1.dp,
            width - 3.dp,
            height - 1.dp
        )
        val dj = (outFrame.bottom - outFrame.top) / 3
        polar.set(
            outFrame.right,
            outFrame.top + dj,
            width - 1.dp,
            outFrame.bottom - dj
        )
        batteryPaint.style = Paint.Style.STROKE
        canvas.drawRect(outFrame, batteryPaint)
        batteryPaint.style = Paint.Style.FILL
        canvas.drawRect(polar, batteryPaint)
    }

}