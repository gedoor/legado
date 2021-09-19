package io.legado.app.ui.widget.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable
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
    internal var width: Float = 0.toFloat()
    internal var height: Float = 0.toFloat()
    var path: String? = null
        private set
    private var defaultCover = true

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

    fun load(path: String? = null) {
        this.path = path
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
        private var showBookName = true
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
                showBookName = appCtx.getPrefBoolean(showNameKey)
                return@let it
            } ?: let {
                showBookName = true
                return@let appCtx.resources.getDrawable(R.drawable.image_cover_default, null)
            }
        }

    }
}
