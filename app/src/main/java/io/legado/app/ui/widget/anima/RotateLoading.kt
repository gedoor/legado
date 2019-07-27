package io.legado.app.ui.widget.anima

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.utils.dp

/**
 * RotateLoading
 * Created by Victor on 2015/4/28.
 */
class RotateLoading : View {

    private lateinit var mPaint: Paint

    private var loadingRectF: RectF? = null
    private var shadowRectF: RectF? = null

    private var topDegree = 10
    private var bottomDegree = 190

    private var arc: Float = 0.toFloat()

    private var thisWidth: Int = 0

    private var changeBigger = true

    private var shadowPosition: Int = 0

    var isStart = false
        private set

    var loadingColor: Int = 0

    private var speedOfDegree: Int = 0

    private var speedOfArc: Float = 0.toFloat()

    constructor(context: Context) : super(context) {
        initView(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        loadingColor = Color.WHITE
        thisWidth = DEFAULT_WIDTH.dp
        shadowPosition = DEFAULT_SHADOW_POSITION.dp
        speedOfDegree = DEFAULT_SPEED_OF_DEGREE

        if (null != attrs) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RotateLoading)
            loadingColor = typedArray.getColor(R.styleable.RotateLoading_loading_color, Color.WHITE)
            thisWidth = typedArray.getDimensionPixelSize(
                R.styleable.RotateLoading_loading_width,
                DEFAULT_WIDTH.dp
            )
            shadowPosition = typedArray.getInt(R.styleable.RotateLoading_shadow_position, DEFAULT_SHADOW_POSITION)
            speedOfDegree = typedArray.getInt(R.styleable.RotateLoading_loading_speed, DEFAULT_SPEED_OF_DEGREE)
            typedArray.recycle()
        }
        speedOfArc = (speedOfDegree / 4).toFloat()
        mPaint = Paint()
        mPaint.color = loadingColor
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = thisWidth.toFloat()
        mPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        arc = 10f

        loadingRectF =
            RectF(
                (2 * thisWidth).toFloat(),
                (2 * thisWidth).toFloat(),
                (w - 2 * thisWidth).toFloat(),
                (h - 2 * thisWidth).toFloat()
            )
        shadowRectF = RectF(
            (2 * thisWidth + shadowPosition).toFloat(),
            (2 * thisWidth + shadowPosition).toFloat(),
            (w - 2 * thisWidth + shadowPosition).toFloat(),
            (h - 2 * thisWidth + shadowPosition).toFloat()
        )
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isStart) {
            return
        }

        mPaint.color = Color.parseColor("#1a000000")
        shadowRectF?.let {
            canvas.drawArc(it, topDegree.toFloat(), arc, false, mPaint)
            canvas.drawArc(it, bottomDegree.toFloat(), arc, false, mPaint)
        }

        mPaint.color = loadingColor
        loadingRectF?.let {
            canvas.drawArc(it, topDegree.toFloat(), arc, false, mPaint)
            canvas.drawArc(it, bottomDegree.toFloat(), arc, false, mPaint)
        }

        topDegree += speedOfDegree
        bottomDegree += speedOfDegree
        if (topDegree > 360) {
            topDegree -= 360
        }
        if (bottomDegree > 360) {
            bottomDegree -= 360
        }

        if (changeBigger) {
            if (arc < 160) {
                arc += speedOfArc
                invalidate()
            }
        } else {
            if (arc > speedOfDegree) {
                arc -= 2 * speedOfArc
                invalidate()
            }
        }
        if (arc >= 160 || arc <= 10) {
            changeBigger = !changeBigger
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isVisible) {
            start()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            start()
        } else {
            stop()
        }
    }

    private fun start() {
        startAnimator()
        isStart = true
        invalidate()
    }

    private fun stop() {
        stopAnimator()
        invalidate()
    }

    private fun startAnimator() {
        val scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", 0.0f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", 0.0f, 1f)
        scaleXAnimator.duration = 300
        scaleXAnimator.interpolator = LinearInterpolator()
        scaleYAnimator.duration = 300
        scaleYAnimator.interpolator = LinearInterpolator()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator)
        animatorSet.start()
    }

    private fun stopAnimator() {
        val scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0f)
        scaleXAnimator.duration = 300
        scaleXAnimator.interpolator = LinearInterpolator()
        scaleYAnimator.duration = 300
        scaleYAnimator.interpolator = LinearInterpolator()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator)
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                isStart = false
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        animatorSet.start()
    }

    companion object {

        private const val DEFAULT_WIDTH = 6
        private const val DEFAULT_SHADOW_POSITION = 2
        private const val DEFAULT_SPEED_OF_DEGREE = 10
    }

}