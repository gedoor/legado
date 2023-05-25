package io.legado.app.model

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.help.CacheManager
import io.legado.app.help.DefaultData
import io.legado.app.help.config.AppConfig
import io.legado.app.help.glide.BlurTransformation
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import splitties.init.appCtx

@Keep
object BookCover {

    private const val coverRuleConfigKey = "legadoCoverRuleConfig"
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
        if (path.isNullOrBlank()) {
            defaultDrawable = appCtx.resources.getDrawable(R.drawable.image_cover_default, null)
            return
        }
        defaultDrawable = kotlin.runCatching {
            BitmapDrawable(appCtx.resources, BitmapUtils.decodeBitmap(path, 600, 900))
        }.getOrDefault(appCtx.resources.getDrawable(R.drawable.image_cover_default, null))
    }

    /**
     * 加载封面
     */
    fun load(
        context: Context,
        path: String?,
        loadOnlyWifi: Boolean = false,
        sourceOrigin: String? = null
    ): RequestBuilder<Drawable> {
        if (AppConfig.useDefaultCover) {
            return ImageLoader.load(context, defaultDrawable)
                .centerCrop()
        }
        var options = RequestOptions().set(OkHttpModelLoader.loadOnlyWifiOption, loadOnlyWifi)
        if (sourceOrigin != null) {
            options = options.set(OkHttpModelLoader.sourceOriginOption, sourceOrigin)
        }
        return ImageLoader.load(context, path)
            .apply(options)
            .placeholder(defaultDrawable)
            .error(defaultDrawable)
            .centerCrop()
    }

    /**
     * 加载模糊封面
     */
    fun loadBlur(
        context: Context,
        path: String?,
        loadOnlyWifi: Boolean = false,
        sourceOrigin: String? = null
    ): RequestBuilder<Drawable> {
        val loadBlur = ImageLoader.load(context, defaultDrawable)
            .transform(BlurTransformation(25), CenterCrop())
        if (AppConfig.useDefaultCover) {
            return loadBlur
        }
        var options = RequestOptions().set(OkHttpModelLoader.loadOnlyWifiOption, loadOnlyWifi)
        if (sourceOrigin != null) {
            options = options.set(OkHttpModelLoader.sourceOriginOption, sourceOrigin)
        }
        return ImageLoader.load(context, path)
            .apply(options)
            .transform(BlurTransformation(25), CenterCrop())
            .transition(DrawableTransitionOptions.withCrossFade(1500))
            .thumbnail(loadBlur)
    }

    fun getCoverRule(): CoverRule {
        return GSON.fromJsonObject<CoverRule>(CacheManager.get(coverRuleConfigKey))
            .getOrNull()
            ?: DefaultData.coverRule
    }

    suspend fun searchCover(book: Book): String? {
        val config = getCoverRule()
        if (!config.enable || config.searchUrl.isBlank() || config.coverRule.isBlank()) {
            return null
        }
        val analyzeUrl = AnalyzeUrl(
            config.searchUrl,
            book.name,
            source = config,
            headerMapF = config.getHeaderMap()
        )
        val res = analyzeUrl.getStrResponseAwait()
        val analyzeRule = AnalyzeRule(book)
        analyzeRule.setContent(res.body)
        analyzeRule.setRedirectUrl(res.url)
        return analyzeRule.getString(config.coverRule, isUrl = true)
    }

    fun saveCoverRule(config: CoverRule) {
        val json = GSON.toJson(config)
        CacheManager.put(coverRuleConfigKey, json)
    }

    fun delCoverRule() {
        CacheManager.delete(coverRuleConfigKey)
    }

    @Keep
    data class CoverRule(
        var enable: Boolean = true,
        var searchUrl: String,
        var coverRule: String,
        override var concurrentRate: String? = null,
        override var loginUrl: String? = null,
        override var loginUi: String? = null,
        override var header: String? = null,
        override var jsLib: String? = null,
        override var enabledCookieJar: Boolean? = false,
    ) : BaseSource {

        override fun getTag(): String {
            return searchUrl
        }

        override fun getKey(): String {
            return searchUrl
        }
    }

}