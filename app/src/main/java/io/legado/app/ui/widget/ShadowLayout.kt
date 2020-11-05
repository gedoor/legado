package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import io.legado.app.R
import io.legado.app.utils.getCompatColor

/**
 * ShadowLayout.java
 *
 * Created by lijiankun on 17/8/11.
 */
@Suppress("unused")
class ShadowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {
    private val mPaint =
        Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRectF = RectF()

    /**
     * 阴影的颜色
     */
    private var mShadowColor = Color.TRANSPARENT

    /**
     * 阴影的大小范围
     */
    private var mShadowRadius = 0f

    /**
     * 阴影 x 轴的偏移量
     */
    private var mShadowDx = 0f

    /**
     * 阴影 y 轴的偏移量
     */
    private var mShadowDy = 0f

    /**
     * 阴影显示的边界
     */
    private var mShadowSide = ALL

    /**
     * 阴影的形状，圆形/矩形
     */
    private var mShadowShape = SHAPE_RECTANGLE


    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null) // 关闭硬件加速
        setWillNotDraw(false) // 调用此方法后，才会执行 onDraw(Canvas) 方法
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.ShadowLayout)
        mShadowColor = typedArray.getColor(
            R.styleable.ShadowLayout_shadowColor,
            context.getCompatColor(android.R.color.black)
        )
        mShadowRadius =
            typedArray.getDimension(R.styleable.ShadowLayout_shadowRadius, dip2px(0f))
        mShadowDx = typedArray.getDimension(R.styleable.ShadowLayout_shadowDx, dip2px(0f))
        mShadowDy = typedArray.getDimension(R.styleable.ShadowLayout_shadowDy, dip2px(0f))
        mShadowSide =
            typedArray.getInt(R.styleable.ShadowLayout_shadowSide, ALL)
        mShadowShape = typedArray.getInt(
            R.styleable.ShadowLayout_shadowShape,
            SHAPE_RECTANGLE
        )
        typedArray.recycle()

        setUpShadowPaint()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val effect = mShadowRadius + dip2px(5f)
        var rectLeft = 0f
        var rectTop = 0f
        var rectRight = this.measuredWidth.toFloat()
        var rectBottom = this.measuredHeight.toFloat()
        var paddingLeft = 0
        var paddingTop = 0
        var paddingRight = 0
        var paddingBottom = 0
        this.width
        if (mShadowSide and LEFT == LEFT) {
            rectLeft = effect
            paddingLeft = effect.toInt()
        }
        if (mShadowSide and TOP == TOP) {
            rectTop = effect
            paddingTop = effect.toInt()
        }
        if (mShadowSide and RIGHT == RIGHT) {
            rectRight = this.measuredWidth - effect
            paddingRight = effect.toInt()
        }
        if (mShadowSide and BOTTOM == BOTTOM) {
            rectBottom = this.measuredHeight - effect
            paddingBottom = effect.toInt()
        }
        if (mShadowDy != 0.0f) {
            rectBottom -= mShadowDy
            paddingBottom += mShadowDy.toInt()
        }
        if (mShadowDx != 0.0f) {
            rectRight -= mShadowDx
            paddingRight += mShadowDx.toInt()
        }
        mRectF.left = rectLeft
        mRectF.top = rectTop
        mRectF.right = rectRight
        mRectF.bottom = rectBottom
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    /**
     * 真正绘制阴影的方法
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        setUpShadowPaint()
        if (mShadowShape == SHAPE_RECTANGLE) {
            canvas.drawRect(mRectF, mPaint)
        } else if (mShadowShape == SHAPE_OVAL) {
            canvas.drawCircle(
                mRectF.centerX(),
                mRectF.centerY(),
                mRectF.width().coerceAtMost(mRectF.height()) / 2,
                mPaint
            )
        }
    }

    fun setShadowColor(shadowColor: Int) {
        mShadowColor = shadowColor
        requestLayout()
        postInvalidate()
    }

    fun setShadowRadius(shadowRadius: Float) {
        mShadowRadius = shadowRadius
        requestLayout()
        postInvalidate()
    }

    private fun setUpShadowPaint() {
        mPaint.reset()
        mPaint.isAntiAlias = true
        mPaint.color = Color.TRANSPARENT
        mPaint.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, mShadowColor)
    }

    /**
     * dip2px dp 值转 px 值
     *
     * @param dpValue dp 值
     * @return px 值
     */
    private fun dip2px(dpValue: Float): Float {
        val dm = context.resources.displayMetrics
        val scale = dm.density
        return dpValue * scale + 0.5f
    }

    companion object {
        const val ALL = 0x1111
        const val LEFT = 0x0001
        const val TOP = 0x0010
        const val RIGHT = 0x0100
        const val BOTTOM = 0x1000
        const val SHAPE_RECTANGLE = 0x0001
        const val SHAPE_OVAL = 0x0010
    }

}