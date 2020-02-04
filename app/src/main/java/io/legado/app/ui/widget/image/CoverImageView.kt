package io.legado.app.ui.widget.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
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
    private var author: String? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSkewX = -0.2f
    }

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
        textPaint.textSize = width / 6
        textPaint.strokeWidth = textPaint.textSize / 10
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
            textPaint.color = Color.WHITE
            textPaint.style = Paint.Style.STROKE
            canvas.drawText(it, width / 2, height * 3 / 5, textPaint)
            textPaint.color = Color.BLACK
            textPaint.style = Paint.Style.FILL
            canvas.drawText(it, width / 2, height * 3 / 5, textPaint)
        }
    }

    fun setName(name: String?, author: String?) {
        this.name =
            when {
                name == null -> null
                name.length > 5 -> name.substring(0, 4) + "…"
                else -> name
            }
        this.author = author
        invalidate()
    }

    fun setHeight(height: Int) {
        val width = height * 5 / 7
        minimumWidth = width
    }

    fun load(path: String?, name: String?, author: String?) {
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
                    setName(name, author)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    setName(null, null)
                    return false
                }

            })
            .centerCrop()
            .into(this)
    }
}
