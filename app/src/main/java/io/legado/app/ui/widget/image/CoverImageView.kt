package io.legado.app.ui.widget.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.legado.app.R
import io.legado.app.help.ImageLoader


class CoverImageView : androidx.appcompat.widget.AppCompatImageView {
    internal var width: Float = 0.toFloat()
    internal var height: Float = 0.toFloat()

    private val textPaint = TextPaint()
    private var name: String? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = measuredWidth * 7 / 5
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        width = getWidth().toFloat()
        height = getHeight().toFloat()
        textPaint.textSize = width / 9
    }

    override fun onDraw(canvas: Canvas) {
        if (width >= 10 && height > 10) {
            @SuppressLint("DrawAllocation")
            val path = Path()
            //四个圆角
            path.moveTo(10f, 0f)
            path.lineTo(width - 10, 0f)
            path.quadTo(width, 0f, width, 10f)
            path.lineTo(width, height - 10)
            path.quadTo(width, height, width - 10, height)
            path.lineTo(10f, height)
            path.quadTo(0f, height, 0f, height - 10)
            path.lineTo(0f, 10f)
            path.quadTo(0f, 0f, 10f, 0f)

            canvas.clipPath(path)
        }
        super.onDraw(canvas)
        name?.let {
            canvas.drawText(it, width / 3, height * 2 / 3, textPaint)
        }
    }

    fun setName(name: String) {
        this.name = name
        invalidate()
    }

    fun setHeight(height: Int) {
        val width = height * 5 / 7
        minimumWidth = width
    }

    fun load(path: String?, name: String) {
        if (path.isNullOrEmpty()) {
            setName(name)
        } else {
            ImageLoader.load(context, path)//Glide自动识别http://和file://
                .placeholder(R.drawable.image_cover_default)
                .error(R.drawable.image_cover_default)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        setName(name)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                })
                .centerCrop()
                .into(this)
        }
    }
}
