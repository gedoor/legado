package io.legado.app.help.exoplayer

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import io.legado.app.help.http.okHttpClient
import splitties.init.appCtx
import java.io.File


object ExoPlayerHelper {

    fun createMediaSource(
        uri: Uri,
        defaultRequestProperties: Map<String, String>
    ): MediaSource {
        val mediaItem = MediaItem.fromUri(uri)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(
            cacheDataSourceFactory.setDefaultRequestProperties(defaultRequestProperties)
        )
        return mediaSourceFactory.createMediaSource(mediaItem)

    }


    /**
     * 支持缓存的DataSource.Factory
     */
    private val cacheDataSourceFactory by lazy {
        //使用自定义的CacheDataSource以支持设置UA
        return@lazy OkhttpCacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(okhttpDataFactory)

    }

    /**
     * Okhttp DataSource.Factory
     */
    private val okhttpDataFactory by lazy {
        OkHttpDataSource.Factory(okHttpClient)
    }

    /**
     * Exoplayer 内置的缓存
     */
    private val cache: Cache by lazy {
        val databaseProvider = ExoDatabaseProvider(appCtx)
        return@lazy SimpleCache(
            //Exoplayer的缓存路径
            File(appCtx.externalCacheDir, "exoplayer"),
            //100M的缓存
            LeastRecentlyUsedCacheEvictor((100 * 1024 * 1024).toLong()),
            //记录缓存的数据库
            databaseProvider
        )

    }

}