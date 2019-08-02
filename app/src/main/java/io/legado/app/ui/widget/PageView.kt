package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.utils.screenshot
import kotlin.math.abs

class PageView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    private var bitmap: Bitmap? = null

    private var downX: Float = 0.toFloat()
    private var offset: Float = 0.toFloat()

    private val srcRect: Rect = Rect()
    private val destRect: Rect = Rect()
    private val shadowDrawable: GradientDrawable

    init {
        val shadowColors = intArrayOf(0x66111111, 0x00000000)
        shadowDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, shadowColors
        )
        shadowDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.save()
        super.onDraw(canvas)
        canvas?.restore()


        bitmap?.let {
            srcRect.set(0, 0, width, height)
            destRect.set(-width + offset.toInt(), 0, offset.toInt(), height)
            canvas?.drawBitmap(it, srcRect, destRect, null)
            addShadow(offset.toInt(), canvas)
        }
    }

    //添加阴影
    private fun addShadow(left: Int, canvas: Canvas?) {
        canvas?.let {
            shadowDrawable.setBounds(left, 0, left + 30, height)
            shadowDrawable.draw(it)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                bitmap = screenshot()
                Log.e("TAG", "bitmap == null: " + (bitmap == null))
                downX = event.x
                offset = 0.toFloat()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                offset = abs(event.x - downX)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                bitmap = null
                invalidate()
            }
        }

        return true
    }
}
