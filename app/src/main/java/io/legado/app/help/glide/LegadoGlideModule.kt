package io.legado.app.help.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import io.legado.app.BuildConfig
import io.legado.app.help.config.AppConfig
import java.io.InputStream


@Suppress("unused")
@GlideModule
class LegadoGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpModeLoaderFactory
        )
        registry.prepend(
            String::class.java,
            InputStream::class.java,
            LegadoDataUrlLoader.Factory()
        )
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        val calculator = MemorySizeCalculator.Builder(context).build()
        val bitmapPool = AsyncRecycleBitmapPool(calculator.bitmapPoolSize)
        builder.setMemorySizeCalculator(calculator)
        builder.setBitmapPool(bitmapPool)
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 1024 * 1024 * 1000))
        if (!BuildConfig.DEBUG && !AppConfig.recordLog) {
            builder.setLogLevel(Log.ERROR)
        }
    }
}