package io.legado.app.ui.widget.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.legado.app.R
import io.legado.app.utils.dpToPx
import kotlin.math.max

class FilletImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    internal var width: Float = 0.toFloat()
    internal var height: Float = 0.toFloat()
    private var leftTopRadius: Int = 0
    private var rightTopRadius: Int = 0
    private var rightBottomRadius: Int = 0
    private var leftBottomRadius: Int = 0

    init {
        // 读取配置
        val array = context.obtainStyledAttributes(attrs, R.styleable.FilletImageView)
        val defaultRadius = 5.dpToPx()
        val radius =
            array.getDimensionPixelOffset(R.styleable.FilletImageView_radius, defaultRadius)
        leftTopRadius = array.getDimensionPixelOffset(
            R.styleable.FilletImageView_left_top_radius,
            defaultRadius
        )
        rightTopRadius = array.getDimensionPixelOffset(
            R.styleable.FilletImageView_right_top_radius,
            defaultRadius
        )
        rightBottomRadius =
            array.getDimensionPixelOffset(
                R.styleable.FilletImageView_right_bottom_radius,
                defaultRadius
            )
        leftBottomRadius = array.getDimensionPixelOffset(
            R.styleable.FilletImageView_left_bottom_radius,
            defaultRadius
        )

        //如果四个角的值没有设置，那么就使用通用的radius的值。
        if (defaultRadius == leftTopRadius) {
            leftTopRadius = radius
        }
        if (defaultRadius == rightTopRadius) {
            rightTopRadius = radius
        }
        if (defaultRadius == rightBottomRadius) {
            rightBottomRadius = radius
        }
        if (defaultRadius == leftBottomRadius) {
            leftBottomRadius = radius
        }
        array.recycle()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        width = getWidth().toFloat()
        height = getHeight().toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        //这里做下判断，只有图片的宽高大于设置的圆角距离的时候才进行裁剪
        val maxLeft = max(leftTopRadius, leftBottomRadius)
        val maxRight = max(rightTopRadius, rightBottomRadius)
        val minWidth = maxLeft + maxRight
        val maxTop = max(leftTopRadius, rightTopRadius)
        val maxBottom = max(leftBottomRadius, rightBottomRadius)
        val minHeight = maxTop + maxBottom
        if (width >= minWidth && height > minHeight) {
            @SuppressLint("DrawAllocation") val path = Path()
            //四个角：右上，右下，左下，左上
            path.moveTo(leftTopRadius.toFloat(), 0f)
            path.lineTo(width - rightTopRadius, 0f)
            path.quadTo(width, 0f, width, rightTopRadius.toFloat())

            path.lineTo(width, height - rightBottomRadius)
            path.quadTo(width, height, width - rightBottomRadius, height)

            path.lineTo(leftBottomRadius.toFloat(), height)
            path.quadTo(0f, height, 0f, height - leftBottomRadius)

            path.lineTo(0f, leftTopRadius.toFloat())
            path.quadTo(0f, 0f, leftTopRadius.toFloat(), 0f)

            canvas.clipPath(path)
        }
        super.onDraw(canvas)
    }

}
