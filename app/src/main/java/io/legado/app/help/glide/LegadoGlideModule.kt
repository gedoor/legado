package io.legado.app.help.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import io.legado.app.BuildConfig
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.FileUtils.createFolderIfNotExist
import java.io.File
import java.io.InputStream

class MultiDiskCacheFactory(
    context: Context,
    defaults: Long
): DiskCache.Factory{

    private val coversCache = DiskLruCacheWrapper.create(createFolderIfNotExist(File(context.filesDir,"covers")),256*1024*100)
    private val defaultsCache = DiskLruCacheWrapper.create(File(DiskCache.Factory.DEFAULT_DISK_CACHE_DIR),defaults)

    override fun build(): DiskCache? {
        return object : DiskCache {
            override fun put(key: Key, writer: DiskCache.Writer) {
                (if (key.toString().contains("covers")) coversCache
                else defaultsCache).put(key, writer)
            }
            override fun get(key: Key?): File? {
                return (if (key.toString().contains("covers")) coversCache
                else defaultsCache).get(key)
            }

            override fun clear() {
                defaultsCache.clear()
            }

            override fun delete(key: Key?) {
                (if (key.toString().contains("covers")) coversCache
                else defaultsCache).delete(key)
            }
        }
    }
}

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
        registry.prepend(
            String::class.java,
            File::class.java,
            FilePathLoader.Factory()
        )
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)
        val calculator = MemorySizeCalculator.Builder(context).build()
        val bitmapPool = AsyncRecycleBitmapPool(calculator.bitmapPoolSize)
        builder.setMemorySizeCalculator(calculator)
        builder.setBitmapPool(bitmapPool)
        builder.setDiskCache(MultiDiskCacheFactory(context, 1024 * 1024 * 1000))
        if (!BuildConfig.DEBUG && !AppConfig.recordLog) {
            builder.setLogLevel(Log.ERROR)
        }
    }
}