package io.legado.app.ui.widget.page

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.R
import io.legado.app.utils.dp
import io.legado.app.utils.screenshot
import kotlinx.android.synthetic.main.view_book_page.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.horizontalPadding

class PageView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private var bitmap: Bitmap? = null

    private var downX: Float = 0.toFloat()
    private var offset: Float = 0.toFloat()

    private val shadowDrawableR: GradientDrawable
    private val shadowDrawableL: GradientDrawable

    private val bitmapMatrix = Matrix()

    private var cover: Boolean = true

    init {
        val shadowColors = intArrayOf(0x66111111, 0x00000000)
        shadowDrawableR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, shadowColors
        )
        shadowDrawableR.gradientType = GradientDrawable.LINEAR_GRADIENT

        shadowDrawableL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT, shadowColors
        )
        shadowDrawableL.gradientType = GradientDrawable.LINEAR_GRADIENT

        inflate(context, R.layout.view_book_page, this)

        setWillNotDraw(false)

        page_panel.backgroundColor = Color.WHITE

        page_panel.horizontalPadding = 16.dp

    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)

        bitmap?.let {
            val dx = if (offset > 0) offset - width else offset + width

            bitmapMatrix.setTranslate(dx, 0.toFloat())
            canvas?.drawBitmap(it, bitmapMatrix, null)

            if (cover) {
                addShadow(dx.toInt(), canvas)
            }
        }
    }

    private fun addShadow(left: Int, canvas: Canvas?) {
        canvas?.let {
            if (left < 0) {
                shadowDrawableR.setBounds(left + width, 0, left+ width + 30, height)
                shadowDrawableR.draw(it)
            } else {
                shadowDrawableL.setBounds(left - 30, 0, left, height)
                shadowDrawableL.draw(it)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                bitmap = page_panel.screenshot()
                downX = event.x
                offset = 0.toFloat()
                if (!cover) {
                    page_panel.translationX = 0.toFloat()
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                offset = event.x - downX
                if (!cover) {
                    page_panel.translationX = offset
                }
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                bitmap = null
                if (!cover) {
                    page_panel.translationX = 0.toFloat()
                }
                invalidate()
            }
        }
        return true
    }

    fun setTranslate(translationX: Float, translationY: Float) {
        page_panel.translationX = translationX
        page_panel.translationY = translationY
    }

    fun setPageFactory(factory: PageFactory<*>) {

    }
}
