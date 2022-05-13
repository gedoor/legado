package io.legado.app.ui.widget.checkbox

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Checkable
import androidx.core.view.postDelayed
import io.legado.app.R
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.dpToPx
import io.legado.app.utils.getCompatColor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class SmoothCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Checkable {
    private var mPaint: Paint
    private var mTickPaint: Paint
    private var mFloorPaint: Paint
    private var mTickPoints: Array<Point>
    private var mCenterPoint: Point
    private var mTickPath: Path
    private var mLeftLineDistance = 0f
    private var mRightLineDistance = 0f
    private var mDrewDistance = 0f
    private var mScaleVal = 1.0f
    private var mFloorScale = 1.0f
    private var mWidth = 0
    private var mAnimDuration = 0
    private var mStrokeWidth = 0
    private var mCheckedColor = 0
    private var mUnCheckedColor = 0
    private var mFloorColor = 0
    private var mFloorUnCheckedColor = 0
    private var mChecked = false
    private var mTickDrawing = false
    var onCheckedChangeListener: ((checkBox: SmoothCheckBox, isChecked: Boolean) -> Unit)? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SmoothCheckBox)
        var tickColor = ThemeStore.accentColor(context)
        mCheckedColor = context.getCompatColor(R.color.background_menu)
        mUnCheckedColor = context.getCompatColor(R.color.background_menu)
        mFloorColor = context.getCompatColor(R.color.transparent30)
        tickColor = ta.getColor(R.styleable.SmoothCheckBox_color_tick, tickColor)
        mAnimDuration = ta.getInt(R.styleable.SmoothCheckBox_duration, DEF_ANIM_DURATION)
        mFloorColor = ta.getColor(R.styleable.SmoothCheckBox_color_unchecked_stroke, mFloorColor)
        mCheckedColor = ta.getColor(R.styleable.SmoothCheckBox_color_checked, mCheckedColor)
        mUnCheckedColor = ta.getColor(R.styleable.SmoothCheckBox_color_unchecked, mUnCheckedColor)
        mStrokeWidth = ta.getDimensionPixelSize(R.styleable.SmoothCheckBox_stroke_width, 0)
        ta.recycle()
        mFloorUnCheckedColor = mFloorColor
        mTickPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mTickPaint.style = Paint.Style.STROKE
        mTickPaint.strokeCap = Paint.Cap.ROUND
        mTickPaint.color = tickColor
        mFloorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mFloorPaint.style = Paint.Style.FILL
        mFloorPaint.color = mFloorColor
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.style = Paint.Style.FILL
        mPaint.color = mCheckedColor
        mTickPath = Path()
        mCenterPoint = Point()
        mTickPoints = arrayOf(Point(), Point(), Point())
        setOnClickListener {
            toggle()
            mTickDrawing = false
            mDrewDistance = 0f
            if (isChecked) {
                startCheckedAnimation()
            } else {
                startUnCheckedAnimation()
            }
        }
    }

    override fun isChecked(): Boolean {
        return mChecked
    }

    override fun setChecked(checked: Boolean) {
        mChecked = checked
        reset()
        invalidate()
        onCheckedChangeListener?.invoke(this@SmoothCheckBox, mChecked)
    }

    override fun toggle() {
        this.isChecked = !isChecked
    }

    /**
     * checked with animation
     *
     * @param checked checked
     * @param animate change with animation
     */
    fun setChecked(checked: Boolean, animate: Boolean) {
        if (animate) {
            mTickDrawing = false
            mChecked = checked
            mDrewDistance = 0f
            if (checked) {
                startCheckedAnimation()
            } else {
                startUnCheckedAnimation()
            }
            onCheckedChangeListener?.invoke(this@SmoothCheckBox, mChecked)
        } else {
            this.isChecked = checked
        }
    }

    private fun reset() {
        mTickDrawing = true
        mFloorScale = 1.0f
        mScaleVal = if (isChecked) 0f else 1.0f
        mFloorColor = if (isChecked) mCheckedColor else mFloorUnCheckedColor
        mDrewDistance = if (isChecked) mLeftLineDistance + mRightLineDistance else 0f
    }

    private fun measureSize(measureSpec: Int): Int {
        val defSize: Int = DEF_DRAW_SIZE.dpToPx()
        val specSize = MeasureSpec.getSize(measureSpec)
        val specMode = MeasureSpec.getMode(measureSpec)
        var result = 0
        when (specMode) {
            MeasureSpec.UNSPECIFIED, MeasureSpec.AT_MOST -> result = min(defSize, specSize)
            MeasureSpec.EXACTLY -> result = specSize
        }
        return result
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measureSize(widthMeasureSpec), measureSize(heightMeasureSpec))
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        mWidth = measuredWidth
        mStrokeWidth = if (mStrokeWidth == 0) measuredWidth / 10 else mStrokeWidth
        mStrokeWidth =
            if (mStrokeWidth > measuredWidth / 5) measuredWidth / 5 else mStrokeWidth
        mStrokeWidth = if (mStrokeWidth < 3) 3 else mStrokeWidth
        mCenterPoint.x = mWidth / 2
        mCenterPoint.y = measuredHeight / 2
        mTickPoints[0].x = (measuredWidth.toFloat() / 30 * 7).roundToInt()
        mTickPoints[0].y = (measuredHeight.toFloat() / 30 * 14).roundToInt()
        mTickPoints[1].x = (measuredWidth.toFloat() / 30 * 13).roundToInt()
        mTickPoints[1].y = (measuredHeight.toFloat() / 30 * 20).roundToInt()
        mTickPoints[2].x = (measuredWidth.toFloat() / 30 * 22).roundToInt()
        mTickPoints[2].y = (measuredHeight.toFloat() / 30 * 10).roundToInt()
        mLeftLineDistance = sqrt(
            (mTickPoints[1].x - mTickPoints[0].x.toDouble()).pow(2.0) +
                    (mTickPoints[1].y - mTickPoints[0].y.toDouble()).pow(2.0)
        ).toFloat()
        mRightLineDistance = sqrt(
            (mTickPoints[2].x - mTickPoints[1].x.toDouble()).pow(2.0) +
                    (mTickPoints[2].y - mTickPoints[1].y.toDouble()).pow(2.0)
        ).toFloat()
        mTickPaint.strokeWidth = mStrokeWidth.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        drawBorder(canvas)
        drawCenter(canvas)
        drawTick(canvas)
    }

    private fun drawCenter(canvas: Canvas) {
        mPaint.color = mUnCheckedColor
        val radius = (mCenterPoint.x - mStrokeWidth) * mScaleVal
        canvas.drawCircle(mCenterPoint.x.toFloat(), mCenterPoint.y.toFloat(), radius, mPaint)
    }

    private fun drawBorder(canvas: Canvas) {
        mFloorPaint.color = mFloorColor
        val radius = mCenterPoint.x
        canvas.drawCircle(
            mCenterPoint.x.toFloat(),
            mCenterPoint.y.toFloat(),
            radius * mFloorScale,
            mFloorPaint
        )
    }

    private fun drawTick(canvas: Canvas) {
        if (mTickDrawing && isChecked) {
            drawTickPath(canvas)
        }
    }

    private fun drawTickPath(canvas: Canvas) {
        mTickPath.reset()
        // draw left of the tick
        if (mDrewDistance < mLeftLineDistance) {
            val step: Float = if (mWidth / 20.0f < 3) 3f else mWidth / 20.0f
            mDrewDistance += step
            val stopX =
                mTickPoints[0].x + (mTickPoints[1].x - mTickPoints[0].x) * mDrewDistance / mLeftLineDistance
            val stopY =
                mTickPoints[0].y + (mTickPoints[1].y - mTickPoints[0].y) * mDrewDistance / mLeftLineDistance
            mTickPath.moveTo(mTickPoints[0].x.toFloat(), mTickPoints[0].y.toFloat())
            mTickPath.lineTo(stopX, stopY)
            canvas.drawPath(mTickPath, mTickPaint)
            if (mDrewDistance > mLeftLineDistance) {
                mDrewDistance = mLeftLineDistance
            }
        } else {
            mTickPath.moveTo(mTickPoints[0].x.toFloat(), mTickPoints[0].y.toFloat())
            mTickPath.lineTo(mTickPoints[1].x.toFloat(), mTickPoints[1].y.toFloat())
            canvas.drawPath(mTickPath, mTickPaint)
            // draw right of the tick
            if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
                val stopX =
                    mTickPoints[1].x + (mTickPoints[2].x - mTickPoints[1].x) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                val stopY =
                    mTickPoints[1].y - (mTickPoints[1].y - mTickPoints[2].y) * (mDrewDistance - mLeftLineDistance) / mRightLineDistance
                mTickPath.reset()
                mTickPath.moveTo(mTickPoints[1].x.toFloat(), mTickPoints[1].y.toFloat())
                mTickPath.lineTo(stopX, stopY)
                canvas.drawPath(mTickPath, mTickPaint)
                val step: Float = if (mWidth / 20f < 3) 3f else mWidth / 20f
                mDrewDistance += step
            } else {
                mTickPath.reset()
                mTickPath.moveTo(mTickPoints[1].x.toFloat(), mTickPoints[1].y.toFloat())
                mTickPath.lineTo(mTickPoints[2].x.toFloat(), mTickPoints[2].y.toFloat())
                canvas.drawPath(mTickPath, mTickPaint)
            }
        }
        // invalidate
        if (mDrewDistance < mLeftLineDistance + mRightLineDistance) {
            postDelayed(10) { this.postInvalidate() }
        }
    }

    private fun startCheckedAnimation() {
        val animator = ValueAnimator.ofFloat(1.0f, 0f)
        animator.duration = mAnimDuration / 3 * 2.toLong()
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation: ValueAnimator ->
            mScaleVal = animation.animatedValue as Float
            mFloorColor = getGradientColor(
                mUnCheckedColor,
                mCheckedColor,
                1 - mScaleVal
            )
            postInvalidate()
        }
        animator.start()
        val floorAnimator = ValueAnimator.ofFloat(1.0f, 0.8f, 1.0f)
        floorAnimator.duration = mAnimDuration.toLong()
        floorAnimator.interpolator = LinearInterpolator()
        floorAnimator.addUpdateListener { animation: ValueAnimator ->
            mFloorScale = animation.animatedValue as Float
            postInvalidate()
        }
        floorAnimator.start()
        drawTickDelayed()
    }

    private fun startUnCheckedAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1.0f)
        animator.duration = mAnimDuration.toLong()
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation: ValueAnimator ->
            mScaleVal = animation.animatedValue as Float
            mFloorColor = getGradientColor(
                mCheckedColor,
                mFloorUnCheckedColor,
                mScaleVal
            )
            postInvalidate()
        }
        animator.start()
        val floorAnimator = ValueAnimator.ofFloat(1.0f, 0.8f, 1.0f)
        floorAnimator.duration = mAnimDuration.toLong()
        floorAnimator.interpolator = LinearInterpolator()
        floorAnimator.addUpdateListener { animation: ValueAnimator ->
            mFloorScale = animation.animatedValue as Float
            postInvalidate()
        }
        floorAnimator.start()
    }

    private fun drawTickDelayed() {
        postDelayed(mAnimDuration.toLong()) {
            mTickDrawing = true
            postInvalidate()
        }
    }

    companion object {
        private const val DEF_DRAW_SIZE = 25
        private const val DEF_ANIM_DURATION = 300
        private fun getGradientColor(startColor: Int, endColor: Int, percent: Float): Int {
            val startA = Color.alpha(startColor)
            val startR = Color.red(startColor)
            val startG = Color.green(startColor)
            val startB = Color.blue(startColor)
            val endA = Color.alpha(endColor)
            val endR = Color.red(endColor)
            val endG = Color.green(endColor)
            val endB = Color.blue(endColor)
            val currentA = (startA * (1 - percent) + endA * percent).toInt()
            val currentR = (startR * (1 - percent) + endR * percent).toInt()
            val currentG = (startG * (1 - percent) + endG * percent).toInt()
            val currentB = (startB * (1 - percent) + endB * percent).toInt()
            return Color.argb(currentA, currentR, currentG, currentB)
        }
    }
}