package io.legado.app.ui.widget.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import io.legado.app.R
import io.legado.app.lib.theme.accentColor

/**
 * 斜角标签
 */
@Suppress("unused")
class BevelLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val MODE_LEFT_TOP = 0
        const val MODE_RIGHT_TOP = 1
        const val MODE_LEFT_BOTTOM = 2
        const val MODE_RIGHT_BOTTOM = 3
        const val MODE_LEFT_TOP_FILL = 4
        const val MODE_RIGHT_TOP_FILL = 5
        const val MODE_LEFT_BOTTOM_FILL = 6
        const val MODE_RIGHT_BOTTOM_FILL = 7
    }

    private var mBgColor: Int
    private var mText: String
    private var mTextSize: Int
    private var mTextColor: Int
    private var mLength: Int
    private var mCorner: Int
    private var mMode: Int
    private var mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var path: Path = Path()
    private var mWidth = 0
    private var mHeight: Int = 0
    private var mRotate = 45 //因为默认模式是1，所以这时是45度

    private var mX: Int = 0
    private var mY: Int = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BevelLabelView)
        mBgColor = typedArray.getColor(
            R.styleable.BevelLabelView_label_bg_color,
            context.accentColor
        ) //默认红色
        mText = typedArray.getString(R.styleable.BevelLabelView_label_text) ?: ""
        mTextSize =
            typedArray.getDimensionPixelOffset(
                R.styleable.BevelLabelView_label_text_size,
                sp2px(11)
            )
        mTextColor = typedArray.getColor(R.styleable.BevelLabelView_label_text_color, Color.WHITE)
        mLength =
            typedArray.getDimensionPixelOffset(R.styleable.BevelLabelView_label_length, dip2px(40))
        mCorner = typedArray.getDimensionPixelOffset(R.styleable.BevelLabelView_label_corner, 0)
        mMode = typedArray.getInt(R.styleable.BevelLabelView_label_mode, 1)
        mPaint.isAntiAlias = true
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = MeasureSpec.getSize(widthMeasureSpec)
        mHeight = mWidth
    }

    override fun onDraw(canvas: Canvas) {
        mPaint.color = mBgColor
        drawBackgroundText(canvas)
    }

    fun setMode(@BevelLabelMode mode: Int) {
        mMode = mode
        invalidate()
    }

    fun setTextColor(@ColorInt color: Int) {
        mTextColor = color
        invalidate()
    }

    fun setBgColor(@ColorInt color: Int) {
        mBgColor = color
        invalidate()
    }

    private fun drawBackgroundText(canvas: Canvas) {
        check(mWidth == mHeight) {
            "width must equal to height" //标签view 是一个正方形，
        }
        when (mMode) {
            MODE_LEFT_TOP -> {
                mCorner = 0 //没有铺满的时候mCorner要归零；
                leftTopMeasure()
                getLeftTop()
            }
            MODE_RIGHT_TOP -> {
                mCorner = 0
                rightTopMeasure()
                getRightTop()
            }
            MODE_LEFT_BOTTOM -> {
                mCorner = 0
                leftBottomMeasure()
                getLeftBottom()
            }
            MODE_RIGHT_BOTTOM -> {
                mCorner = 0
                rightBottomMeasure()
                getRightBottom()
            }
            MODE_LEFT_TOP_FILL -> {
                leftTopMeasure()
                getLeftTopFill()
                if (mCorner != 0) {
                    canvas.drawPath(path, mPaint)
                    getLeftTop()
                }
            }
            MODE_RIGHT_TOP_FILL -> {
                rightTopMeasure()
                getRightTopFill()
                if (mCorner != 0) {
                    canvas.drawPath(path, mPaint)
                    getRightTop()
                }
            }
            MODE_LEFT_BOTTOM_FILL -> {
                leftBottomMeasure()
                getLeftBottomFill()
                if (mCorner != 0) {
                    canvas.drawPath(path, mPaint)
                    getLeftBottom()
                }
            }
            MODE_RIGHT_BOTTOM_FILL -> {
                rightBottomMeasure()
                getRightBottomFill()
                if (mCorner != 0) {
                    canvas.drawPath(path, mPaint)
                    getRightBottom()
                }
            }
            else -> {}
        }
        canvas.drawPath(path, mPaint)
        mPaint.textSize = mTextSize.toFloat()
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.color = mTextColor
        canvas.translate(mX.toFloat(), mY.toFloat())
        canvas.rotate(mRotate.toFloat())
        val baseLineY = (-(mPaint.descent() + mPaint.ascent())).toInt() / 2 //基线中间点的y轴计算公式
        canvas.drawText(mText, 0f, baseLineY.toFloat(), mPaint)
    }

    private fun rightBottomMeasure() {
        mRotate = -45
        mX = mWidth / 2 + mLength / 4
        mY = mX
    }

    private fun leftBottomMeasure() {
        mRotate = 45
        mX = mWidth / 2 - mLength / 4
        mY = mHeight / 2 + mLength / 4
    }

    private fun rightTopMeasure() {
        mRotate = 45
        mX = mWidth / 2 + mLength / 4
        mY = mHeight / 2 - mLength / 4
    }

    private fun leftTopMeasure() {
        mRotate = -45
        mX = mWidth / 2 - mLength / 4
        mY = mX
    }

    //左上角铺满
    private fun getLeftTopFill() {
        if (mCorner != 0) {
            path.addRoundRect(
                0f,
                0f,
                (mWidth / 2).toFloat(),
                (mHeight / 2).toFloat(),
                floatArrayOf(mCorner.toFloat(), mCorner.toFloat(), 0f, 0f, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        } else {
            path.moveTo(0f, 0f)
            path.lineTo(mWidth.toFloat(), 0f)
            path.lineTo(0f, mHeight.toFloat())
            path.close()
        }
    }

    //左上角不铺满
    private fun getLeftTop() {
        path.moveTo(if (mCorner != 0) mCorner.toFloat() else (mWidth - mLength).toFloat(), 0f)
        path.lineTo(mWidth.toFloat(), 0f)
        path.lineTo(0f, mHeight.toFloat())
        path.lineTo(0f, if (mCorner != 0) mCorner.toFloat() else (mHeight - mLength).toFloat())
        path.close()
    }

    //左下角铺满
    private fun getLeftBottomFill() {
        if (mCorner != 0) {
            path.addRoundRect(
                0f,
                (mHeight / 2).toFloat(),
                (mWidth / 2).toFloat(),
                mHeight.toFloat(),
                floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, mCorner.toFloat(), mCorner.toFloat()),
                Path.Direction.CW
            )
        } else {
            path.moveTo(0f, 0f)
            path.lineTo(mWidth.toFloat(), mHeight.toFloat())
            path.lineTo(0f, mHeight.toFloat())
            path.close()
        }
    }


    //左下角不铺满
    private fun getLeftBottom() {
        path.moveTo(0f, 0f)
        path.lineTo(mWidth.toFloat(), mHeight.toFloat())
        path.lineTo(
            if (mCorner != 0) mCorner.toFloat() else (mWidth - mLength).toFloat(),
            mHeight.toFloat()
        )
        path.lineTo(0f, if (mCorner != 0) (mHeight - mCorner).toFloat() else mLength.toFloat())
        path.close()
    }

    //右上角铺满
    private fun getRightTopFill() {
        if (mCorner != 0) {
            path.addRoundRect(
                (mWidth / 2).toFloat(),
                0f,
                mWidth.toFloat(),
                (mHeight / 2).toFloat(),
                floatArrayOf(0f, 0f, mCorner.toFloat(), mCorner.toFloat(), 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        } else {
            path.moveTo(0f, 0f)
            path.lineTo(mWidth.toFloat(), 0f)
            path.lineTo(mWidth.toFloat(), mHeight.toFloat())
            path.close()
        }
    }

    //右上角不铺满
    private fun getRightTop() {
        path.moveTo(0f, 0f)
        path.lineTo(if (mCorner != 0) (mWidth - mCorner).toFloat() else mLength.toFloat(), 0f)
        path.lineTo(
            mWidth.toFloat(),
            if (mCorner != 0) mCorner.toFloat() else (mHeight - mLength).toFloat()
        )
        path.lineTo(mWidth.toFloat(), mHeight.toFloat())
        path.close()
    }

    //右下角铺满
    private fun getRightBottomFill() {
        if (mCorner != 0) {
            path.addRoundRect(
                (mWidth / 2).toFloat(),
                (mHeight / 2).toFloat(),
                mWidth.toFloat(),
                mHeight.toFloat(),
                floatArrayOf(0f, 0f, 0f, 0f, mCorner.toFloat(), mCorner.toFloat(), 0f, 0f),
                Path.Direction.CW
            )
        } else {
            path.moveTo(mWidth.toFloat(), 0f)
            path.lineTo(mWidth.toFloat(), mHeight.toFloat())
            path.lineTo(0f, mHeight.toFloat())
            path.close()
        }
    }

    //右下角不铺满
    private fun getRightBottom() {
        path.moveTo(mWidth.toFloat(), 0f)
        path.lineTo(
            mWidth.toFloat(),
            if (mCorner != 0) (mHeight - mCorner).toFloat() else mLength.toFloat()
        )
        path.lineTo(
            if (mCorner != 0) (mWidth - mCorner).toFloat() else mLength.toFloat(),
            mHeight.toFloat()
        )
        path.lineTo(0f, mHeight.toFloat())
        path.close()
    }


    /**
     * @param sp 转换大小
     */
    @Suppress("SameParameterValue")
    private fun sp2px(sp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            resources.displayMetrics
        )
            .toInt()
    }

    /**
     * @param dip 转换大小
     */
    @Suppress("SameParameterValue")
    private fun dip2px(dip: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dip.toFloat(),
            resources.displayMetrics
        )
            .toInt()
    }

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        MODE_LEFT_BOTTOM, MODE_LEFT_BOTTOM_FILL, MODE_LEFT_TOP, MODE_LEFT_TOP_FILL,
        MODE_RIGHT_BOTTOM, MODE_RIGHT_BOTTOM_FILL, MODE_RIGHT_TOP, MODE_RIGHT_TOP_FILL
    )
    annotation class BevelLabelMode

}