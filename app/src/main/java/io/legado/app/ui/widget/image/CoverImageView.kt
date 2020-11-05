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
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.ImageLoader
import io.legado.app.utils.getPrefString

/**
 * 封面
 */
@Suppress("unused")
class CoverImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatImageView(
    context,
    attrs
) {
    internal var width: Float = 0.toFloat()
    internal var height: Float = 0.toFloat()
    private var nameHeight = 0f
    private var authorHeight = 0f
    private val namePaint by lazy {
        val textPaint = TextPaint()
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSkewX = -0.2f
        textPaint
    }
    private val authorPaint by lazy {
        val textPaint = TextPaint()
        textPaint.typeface = Typeface.DEFAULT
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSkewX = -0.1f
        textPaint
    }
    private var name: String? = null
    private var author: String? = null
    private var loadFailed = false

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
        namePaint.textSize = width / 6
        namePaint.strokeWidth = namePaint.textSize / 10
        authorPaint.textSize = width / 9
        authorPaint.strokeWidth = authorPaint.textSize / 10
        val fm = namePaint.fontMetrics
        nameHeight = height * 0.5f + (fm.bottom - fm.top) * 0.5f
        authorHeight = nameHeight + (fm.bottom - fm.top) * 0.6f
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
        if (!loadFailed || !showBookName) return
        name?.let {
            namePaint.color = Color.WHITE
            namePaint.style = Paint.Style.STROKE
            canvas.drawText(it, width / 2, nameHeight, namePaint)
            namePaint.color = Color.RED
            namePaint.style = Paint.Style.FILL
            canvas.drawText(it, width / 2, nameHeight, namePaint)
        }
        author?.let {
            authorPaint.color = Color.WHITE
            authorPaint.style = Paint.Style.STROKE
            canvas.drawText(it, width / 2, authorHeight, authorPaint)
            authorPaint.color = Color.RED
            authorPaint.style = Paint.Style.FILL
            canvas.drawText(it, width / 2, authorHeight, authorPaint)
        }
    }

    fun setHeight(height: Int) {
        val width = height * 5 / 7
        minimumWidth = width
    }

    private fun setText(name: String?, author: String?) {
        this.name =
            when {
                name == null -> null
                name.length > 5 -> name.substring(0, 4) + "…"
                else -> name
            }
        this.author =
            when {
                author == null -> null
                author.length > 8 -> author.substring(0, 7) + "…"
                else -> author
            }
    }

    fun load(path: String?, name: String?, author: String?) {
        setText(name, author)
        ImageLoader.load(context, path)//Glide自动识别http://,content://和file://
            .placeholder(defaultDrawable)
            .error(defaultDrawable)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    loadFailed = true
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    loadFailed = false
                    return false
                }

            })
            .centerCrop()
            .into(this)
    }

    companion object {
        private var showBookName = false
        lateinit var defaultDrawable: Drawable

        init {
            upDefaultCover()
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun upDefaultCover() {
            val path = App.INSTANCE.getPrefString(PreferKey.defaultCover)
            var dw = Drawable.createFromPath(path)
            if (dw == null) {
                showBookName = true
                dw = App.INSTANCE.resources.getDrawable(R.drawable.image_cover_default, null)
            } else {
                showBookName = false
            }
            defaultDrawable = dw!!
        }

    }
}
