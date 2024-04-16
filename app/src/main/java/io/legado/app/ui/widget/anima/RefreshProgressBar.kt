package io.legado.app.ui.widget.anima

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.util.AttributeSet
import android.view.View

import io.legado.app.R

@Suppress("unused", "MemberVisibilityCanBePrivate")
class RefreshProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var a = 1
    private var durProgress = 0
    private var secondDurProgress = 0
    var maxProgress = 100
    var secondMaxProgress = 100
    var bgColor = 0x00000000
    var secondColor = -0x3e3e3f
    var fontColor = -0xc9c9ca
    var speed = 2
    var secondFinalProgress = 0
        private set
    private var paint: Paint = Paint()
    private val bgRect = Rect()
    private val secondRect = Rect()
    private val fontRectF = RectF()

    var isAutoLoading: Boolean = false
        set(loading) {
            field = loading
            if (!loading) {
                secondDurProgress = 0
                secondFinalProgress = 0
            }
            maxProgress = 0

            invalidate()
        }

    init {
        paint.style = Paint.Style.FILL

        val a = context.obtainStyledAttributes(attrs, R.styleable.RefreshProgressBar)
        speed = a.getDimensionPixelSize(R.styleable.RefreshProgressBar_speed, speed)
        maxProgress = a.getInt(R.styleable.RefreshProgressBar_max_progress, maxProgress)
        durProgress = a.getInt(R.styleable.RefreshProgressBar_dur_progress, durProgress)
        secondDurProgress = a.getDimensionPixelSize(
            R.styleable.RefreshProgressBar_second_dur_progress,
            secondDurProgress
        )
        secondFinalProgress = secondDurProgress
        secondMaxProgress = a.getDimensionPixelSize(
            R.styleable.RefreshProgressBar_second_max_progress,
            secondMaxProgress
        )
        bgColor = a.getColor(R.styleable.RefreshProgressBar_bg_color, bgColor)
        secondColor = a.getColor(R.styleable.RefreshProgressBar_second_color, secondColor)
        fontColor = a.getColor(R.styleable.RefreshProgressBar_font_color, fontColor)
        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.color = bgColor
        bgRect.set(0, 0, measuredWidth, measuredHeight)
        canvas.drawRect(bgRect, paint)

        if (secondDurProgress > 0 && secondMaxProgress > 0) {
            var secondDur = secondDurProgress
            if (secondDur > secondMaxProgress) {
                secondDur = secondMaxProgress
            }
            paint.color = secondColor
            val tempW =
                (measuredWidth.toFloat() * 1.0f * (secondDur * 1.0f / secondMaxProgress)).toInt()
            secondRect.set(
                measuredWidth / 2 - tempW / 2,
                0,
                measuredWidth / 2 + tempW / 2,
                measuredHeight
            )
            canvas.drawRect(secondRect, paint)
        }

        if (durProgress > 0 && maxProgress > 0) {
            paint.color = fontColor
            fontRectF.set(
                0f,
                0f,
                measuredWidth.toFloat() * 1.0f * (durProgress * 1.0f / maxProgress),
                measuredHeight.toFloat()
            )
            canvas.drawRect(fontRectF, paint)
        }

        if (this.isAutoLoading) {
            if (secondDurProgress >= secondMaxProgress) {
                a = -1
            } else if (secondDurProgress <= 0) {
                a = 1
            }
            secondDurProgress += a * speed
            if (secondDurProgress < 0)
                secondDurProgress = 0
            else if (secondDurProgress > secondMaxProgress)
                secondDurProgress = secondMaxProgress
            secondFinalProgress = secondDurProgress
            invalidate()
        } else {
            if (secondDurProgress != secondFinalProgress) {
                if (secondDurProgress > secondFinalProgress) {
                    secondDurProgress -= speed
                    if (secondDurProgress < secondFinalProgress) {
                        secondDurProgress = secondFinalProgress
                    }
                } else {
                    secondDurProgress += speed
                    if (secondDurProgress > secondFinalProgress) {
                        secondDurProgress = secondFinalProgress
                    }
                }
                this.invalidate()
            }
        }
    }

    fun getDurProgress(): Int {
        return durProgress
    }

    fun setDurProgress(durProgress: Int) {
        var durProgress1 = durProgress
        if (durProgress1 < 0) {
            durProgress1 = 0
        }
        if (durProgress1 > maxProgress) {
            durProgress1 = maxProgress
        }
        this.durProgress = durProgress1
        if (Looper.myLooper() == Looper.getMainLooper()) {
            this.invalidate()
        } else {
            this.postInvalidate()
        }
    }

    fun getSecondDurProgress(): Int {
        return secondDurProgress
    }

    fun setSecondDurProgress(secondDur: Int) {
        this.secondDurProgress = secondDur
        this.secondFinalProgress = secondDurProgress
        if (Looper.myLooper() == Looper.getMainLooper()) {
            this.invalidate()
        } else {
            this.postInvalidate()
        }
    }

    fun setSecondDurProgressWithAnim(secondDur: Int) {
        var secondDur1 = secondDur
        if (secondDur1 < 0) {
            secondDur1 = 0
        }
        if (secondDur1 > secondMaxProgress) {
            secondDur1 = secondMaxProgress
        }
        this.secondFinalProgress = secondDur1
        if (Looper.myLooper() == Looper.getMainLooper()) {
            this.invalidate()
        } else {
            this.postInvalidate()
        }
    }

    fun clean() {
        durProgress = 0
        secondDurProgress = 0
        secondFinalProgress = 0
        if (Looper.myLooper() == Looper.getMainLooper()) {
            this.invalidate()
        } else {
            this.postInvalidate()
        }
    }
}
