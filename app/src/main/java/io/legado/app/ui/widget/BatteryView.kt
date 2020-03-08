package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import io.legado.app.R
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.sp

class BatteryView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var battery = 100
    private val textPaint = TextPaint()
    private var batteryHeight: Int = 0
    private var batteryWidth: Int = 0
    private val outFrame = Rect()
    private val polar = Rect()

    init {
        textPaint.textSize = 10.sp.toFloat()
        textPaint.strokeWidth = 1.dp.toFloat()
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = context.getCompatColor(R.color.tv_text_default)
        textPaint.typeface = Typeface.createFromAsset(context.assets, "number.ttf")
        batteryHeight = with(textPaint.fontMetrics) { descent - ascent + leading }.toInt()
        batteryWidth = StaticLayout.getDesiredWidth("100", textPaint).toInt() + 10.dp
        outFrame.set(1.dp, 1.dp, batteryWidth - 3.dp, batteryHeight - 1.dp)
        polar.set(outFrame.right, batteryHeight / 3, batteryWidth, batteryHeight * 2 / 3)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(batteryWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(batteryHeight, MeasureSpec.EXACTLY)
        )
    }

    fun setColor(@ColorInt color: Int) {
        textPaint.color = color
        invalidate()
    }

    fun setBattery(battery: Int) {
        this.battery = battery
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        textPaint.style = Paint.Style.STROKE
        canvas.drawRect(outFrame, textPaint)
        textPaint.style = Paint.Style.FILL
        canvas.drawRect(polar, textPaint)
        val text = battery.toString()
        val baseHeight = batteryHeight - textPaint.fontMetrics.descent
        canvas.drawText(text, outFrame.right / 2.toFloat(), baseHeight, textPaint)
    }

}