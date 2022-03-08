package io.legado.app.model

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.BaseSource
import io.legado.app.help.BlurTransformation
import io.legado.app.help.CacheManager
import io.legado.app.help.config.AppConfig
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx

object BookCover {

    private const val coverRuleConfigKey = "legadoCoverRuleConfig"
    var drawBookName = true
        private set
    var drawBookAuthor = true
        private set
    lateinit var defaultDrawable: Drawable
        private set
    var coverRuleConfig: CoverRuleConfig? =
        GSON.fromJsonObject<CoverRuleConfig>(CacheManager.get(coverRuleConfigKey)).getOrNull()
    private val analyzeRule by lazy {
        AnalyzeRule()
    }

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

    fun getBlurDefaultCover(context: Context): RequestBuilder<Drawable> {
        return ImageLoader.load(context, defaultDrawable)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(context, 25)))
    }

    fun searchCover(name: String, author: String): String? {
        val config = coverRuleConfig ?: return null
        if (config.searchUrl.isBlank() || config.coverRule.isBlank()) {
            return null
        }
        val analyzeUrl =
            AnalyzeUrl(config.searchUrl, name, source = config, headerMapF = config.getHeaderMap())
        return runBlocking {
            analyzeUrl.getStrResponseAwait().body?.let { body ->
                return@let analyzeRule.getString(config.coverRule, body, true)
            }
        }
    }

    fun saveCoverRuleConfig(config: CoverRuleConfig) {
        coverRuleConfig = config
        val json = GSON.toJson(config)
        CacheManager.put(coverRuleConfigKey, json)
    }

    data class CoverRuleConfig(
        var searchUrl: String,
        var coverRule: String,
        override var concurrentRate: String? = null,
        override var loginUrl: String? = null,
        override var loginUi: String? = null,
        override var header: String? = null,
    ) : BaseSource {

        override fun getTag(): String {
            return searchUrl
        }

        override fun getKey(): String {
            return searchUrl
        }
    }

}