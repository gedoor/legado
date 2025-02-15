package io.legado.app.help.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
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
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        builder.setMemoryCache(LruResourceCache(1024 * 1024 * 500))
            .setBitmapPool(LruBitmapPool(1024 * 1024 * 200))
            .setDiskCache(
                InternalCacheDiskCacheFactory(
                    context,
                    1024 * 1024 * 1000
                )
            )
            .setDefaultRequestOptions {
                RequestOptions().format(DecodeFormat.PREFER_RGB_565)
                    .encodeQuality(90)
            }
    }
}