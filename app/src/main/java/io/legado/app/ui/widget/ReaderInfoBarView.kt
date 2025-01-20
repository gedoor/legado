package io.legado.app.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.legado.app.R
import splitties.dimensions.dp
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.R as materialR

class ReaderInfoBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBounds = Rect()
    private val timeFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT)
    private val timeReceiver = TimeReceiver()
    private var insetLeft: Int = 0
    private var insetRight: Int = 0
    private var insetTop: Int = 0
    private var cutoutInsetLeft = 0
    private var cutoutInsetRight = 0
    private val colorText = ColorUtils.setAlphaComponent(
        context.obtainStyledAttributes(intArrayOf(materialR.attr.colorOnSurface)).use {
            it.getColor(0, Color.BLACK)
        },
        200,
    )
    private val colorOutline = ColorUtils.setAlphaComponent(
        context.obtainStyledAttributes(intArrayOf(materialR.attr.colorSurface)).use {
            it.getColor(0, Color.WHITE)
        },
        200,
    )

    private var timeText = timeFormat.format(Date())
    private var text: String = ""

    private val innerHeight
        get() = height - paddingTop - paddingBottom - insetTop

    private val innerWidth
        get() = width - paddingLeft - paddingRight - insetLeft - insetRight

    init {
        val insetStart = dp(10f).toInt()
        val insetEnd = dp(10f).toInt()
        paint.strokeWidth = dp(2f)
        paint.setShadowLayer(2f, 1f, 1f, Color.GRAY)
        insetLeft = insetStart
        insetRight = insetEnd
        insetTop = minOf(insetLeft, insetRight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val ty = innerHeight / 2f + textBounds.height() / 2f - textBounds.bottom
        paint.textAlign = Paint.Align.LEFT
        canvas.drawTextOutline(
            text,
            (paddingLeft + insetLeft + cutoutInsetLeft).toFloat(),
            paddingTop + insetTop + ty,
        )
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawTextOutline(
            timeText,
            (width - paddingRight - insetRight - cutoutInsetRight).toFloat(),
            paddingTop + insetTop + ty,
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateCutoutInsets(ViewCompat.getRootWindowInsets(this))
        updateTextSize()
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        updateCutoutInsets(WindowInsetsCompat.toWindowInsetsCompat(insets))
        return super.onApplyWindowInsets(insets)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ContextCompat.registerReceiver(
            context,
            timeReceiver,
            IntentFilter(Intent.ACTION_TIME_TICK),
            ContextCompat.RECEIVER_EXPORTED,
        )
        updateCutoutInsets(ViewCompat.getRootWindowInsets(this))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(timeReceiver)
    }

    fun update(
        currentPage: Int,
        totalPage: Int,
        percent: Float,
        chapterIndex: Int,
        chapterCount: Int
    ) {
        text = context.getString(
            R.string.book_reader_info_bar,
            chapterIndex,
            chapterCount,
            currentPage,
            totalPage,
            if (percent in 0f..1f) (percent * 100).format() else ""
        )
        updateTextSize()
        invalidate()
    }

    private fun Number.format(
        decimals: Int = 0,
        decPoint: Char = '.',
        thousandsSep: Char? = ' '
    ): String {
        val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
        val symbols = formatter.decimalFormatSymbols
        if (thousandsSep != null) {
            symbols.groupingSeparator = thousandsSep
            formatter.isGroupingUsed = true
        } else {
            formatter.isGroupingUsed = false
        }
        symbols.decimalSeparator = decPoint
        formatter.decimalFormatSymbols = symbols
        formatter.minimumFractionDigits = decimals
        formatter.maximumFractionDigits = decimals
        return when (this) {
            is Float,
            is Double,
                -> formatter.format(this.toDouble())

            else -> formatter.format(this.toLong())
        }
    }


    private fun updateTextSize() {
        val str = text + timeText
        val testTextSize = 48f
        paint.textSize = testTextSize
        paint.getTextBounds(str, 0, str.length, textBounds)
        paint.textSize = testTextSize * innerHeight / textBounds.height()
        paint.getTextBounds(str, 0, str.length, textBounds)
    }

    private fun Canvas.drawTextOutline(text: String, x: Float, y: Float) {
        paint.color = colorOutline
        paint.style = Paint.Style.STROKE
        drawText(text, x, y, paint)
        paint.color = colorText
        paint.style = Paint.Style.FILL
        drawText(text, x, y, paint)
    }

    private fun updateCutoutInsets(insetsCompat: WindowInsetsCompat?) {
        val cutouts = (insetsCompat ?: return).displayCutout?.boundingRects.orEmpty()
        cutoutInsetLeft = 0
        cutoutInsetRight = 0
        for (rect in cutouts) {
            if (rect.left <= paddingLeft) {
                cutoutInsetLeft += rect.width()
            }
            if (rect.right >= width - paddingRight) {
                cutoutInsetRight += rect.width()
            }
        }
    }

    private inner class TimeReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            timeText = timeFormat.format(Date())
            invalidate()
        }
    }
}
