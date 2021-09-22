package io.legado.app.model

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.BlurTransformation
import io.legado.app.help.glide.ImageLoader
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import splitties.init.appCtx

object BookCover {

    var drawBookName = true
        private set
    var drawBookAuthor = true
        private set
    lateinit var defaultDrawable: Drawable
        private set

    init {
        upDefaultCover()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun upDefaultCover() {
        val isNightTheme = AppConfig.isNightTheme
        drawBookName = if (isNightTheme) {
            appCtx.getPrefBoolean(PreferKey.coverShowNameN, true)
        } else {
            appCtx.getPrefBoolean(PreferKey.coverShowName, true)
        }
        drawBookAuthor = if (isNightTheme) {
            appCtx.getPrefBoolean(PreferKey.coverShowAuthorN, true)
        } else {
            appCtx.getPrefBoolean(PreferKey.coverShowAuthor, true)
        }
        val key = if (isNightTheme) PreferKey.defaultCoverDark else PreferKey.defaultCover
        val path = appCtx.getPrefString(key)
        defaultDrawable = Drawable.createFromPath(path)
            ?: appCtx.resources.getDrawable(R.drawable.image_cover_default, null)
    }

    fun getBlurDefaultCover(context: Context): RequestBuilder<Drawable> {
        return ImageLoader.load(context, defaultDrawable)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(context, 25)))
    }

}