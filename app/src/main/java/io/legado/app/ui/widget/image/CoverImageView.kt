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
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.glide.ImageLoader
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.textHeight
import io.legado.app.utils.toStringArray
import splitties.init.appCtx

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
    private var filletPath = Path()
    private var width: Float = 0.toFloat()
    private var height: Float = 0.toFloat()
    private var defaultCover = true
    var bitmapPath: String? = null
        private set
    private var name: String? = null
    private var author: String? = null
    private var nameHeight = 0f
    private var authorHeight = 0f
    private val namePaint by lazy {
        val textPaint = TextPaint()
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint
    }
    private val authorPaint by lazy {
        val textPaint = TextPaint()
        textPaint.typeface = Typeface.DEFAULT
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
        textPaint
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
        filletPath.reset()
        if (width > 10 && height > 10) {
            filletPath.apply {
                moveTo(10f, 0f)
                lineTo(width - 10, 0f)
                quadTo(width, 0f, width, 10f)
                lineTo(width, height - 10)
                quadTo(width, height, width - 10, height)
                lineTo(10f, height)
                quadTo(0f, height, 0f, height - 10)
                lineTo(0f, 10f)
                quadTo(0f, 0f, 10f, 0f)
                close()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!filletPath.isEmpty) {
            canvas.clipPath(filletPath)
        }
        super.onDraw(canvas)
        if (defaultCover && drawBookName) {
            drawName(canvas)
        }
    }

    private fun drawName(canvas: Canvas) {
        var startX = width * 0.2f
        var startY = height * 0.2f
        name?.toStringArray()?.let { name ->
            namePaint.textSize = width / 7
            namePaint.strokeWidth = namePaint.textSize / 10
            name.forEach {
                namePaint.color = Color.WHITE
                namePaint.style = Paint.Style.STROKE
                canvas.drawText(it, startX, startY, namePaint)
                namePaint.color = Color.DKGRAY
                namePaint.style = Paint.Style.FILL
                canvas.drawText(it, startX, startY, namePaint)
                startY += namePaint.textHeight
                if (startY > height * 0.8) {
                    return@let
                }
            }
        }
        author?.toStringArray()?.let { author ->
            startX = width * 0.8f
            startY = height * 0.7f
            authorPaint.textSize = width / 9
            authorPaint.strokeWidth = authorPaint.textSize / 10
            author.forEach {
                authorPaint.color = Color.WHITE
                authorPaint.style = Paint.Style.STROKE
                canvas.drawText(it, startX, startY, authorPaint)
                authorPaint.color = Color.DKGRAY
                authorPaint.style = Paint.Style.FILL
                canvas.drawText(it, startX, startY, authorPaint)
                startY += authorPaint.textHeight
                if (startY > height * 0.95) {
                    return@let
                }
            }
        }
    }

    fun setHeight(height: Int) {
        val width = height * 5 / 7
        minimumWidth = width
    }

    private val glideListener by lazy {
        object : RequestListener<Drawable> {

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                defaultCover = true
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                defaultCover = false
                return false
            }

        }
    }

    fun load(path: String? = null, name: String? = null, author: String? = null) {
        this.bitmapPath = path
        this.name = name
        this.author = author
        if (AppConfig.useDefaultCover) {
            ImageLoader.load(context, defaultDrawable)
                .centerCrop()
                .into(this)
        } else {
            ImageLoader.load(context, path)//Glide自动识别http://,content://和file://
                .placeholder(defaultDrawable)
                .error(defaultDrawable)
                .listener(glideListener)
                .centerCrop()
                .into(this)
        }
    }

    companion object {
        private var drawBookName = true
        lateinit var defaultDrawable: Drawable

        init {
            upDefaultCover()
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun upDefaultCover() {
            val isNightTheme = AppConfig.isNightTheme
            val key = if (isNightTheme) PreferKey.defaultCoverDark else PreferKey.defaultCover
            val path = appCtx.getPrefString(key)
            defaultDrawable = Drawable.createFromPath(path)?.let {
                val showNameKey = if (isNightTheme) PreferKey.defaultCoverDarkShowName
                else PreferKey.defaultCoverShowName
                drawBookName = appCtx.getPrefBoolean(showNameKey)
                return@let it
            } ?: let {
                drawBookName = true
                return@let appCtx.resources.getDrawable(R.drawable.image_cover_default, null)
            }
        }

    }
}
