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
import io.legado.app.utils.dp

class BatteryView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var battery = 100
    private val textPaint = TextPaint()
    private var batteryHeight: Int = 0
    private var batteryWidth: Int = 0
    private val outFrame = Rect()
    private val polar = Rect()

    init {
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.createFromAsset(context.assets, "number.ttf")
        batteryHeight = with(textPaint.fontMetrics) { descent - ascent + leading }.toInt() + 4.dp
        batteryWidth = StaticLayout.getDesiredWidth("100", textPaint).toInt() + 5.dp
        outFrame.set(0, 0, batteryWidth - 2.dp, batteryHeight)
        polar.set(outFrame.right, batteryHeight / 4, batteryWidth, batteryHeight * 3 / 4)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(batteryWidth, batteryHeight)
    }

    fun setBattery(battery: Int) {
        this.battery = battery
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(polar, textPaint)
        textPaint.style = Paint.Style.STROKE
        canvas.drawRect(outFrame, textPaint)
        val text = battery.toString()
        canvas.drawText(text, outFrame.right / 2.toFloat(), batteryHeight / 2.toFloat(), textPaint)
    }

}