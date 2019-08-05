package io.legado.app.ui.widget.page

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.utils.screenshot
import kotlin.math.abs

class PageView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

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

        inflate(context, R.layout.page_view, this)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.save()
        super.onDraw(canvas)
        canvas?.restore()


    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return true
    }

    fun setPageFactory(factory: PageFactory<*>){

    }
}
