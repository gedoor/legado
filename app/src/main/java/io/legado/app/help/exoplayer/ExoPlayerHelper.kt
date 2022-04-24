package io.legado.app.help.exoplayer

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloaderFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.*
import io.legado.app.help.http.okHttpClient
import okhttp3.CacheControl
import splitties.init.appCtx
import java.io.File
import java.util.concurrent.TimeUnit


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
     * 预下载
     * @param uri 音频资源uri
     * @param defaultRequestProperties 请求头
     * @param progressCallBack 下载进度回调
     */
    fun preDownload(
        uri: Uri,
        defaultRequestProperties: Map<String, String>,
        progressCallBack: (contentLength: Long, bytesDownloaded: Long, percentDownloaded: Float) -> Unit = { _: Long, _: Long, _: Float -> }
    ) {
        val request = DownloadRequest.Builder(uri.toString(), uri).build()
        cacheDataSourceFactory.setDefaultRequestProperties(defaultRequestProperties)
        okHttpClient.dispatcher.executorService.submit {
            downloaderFactory.createDownloader(request)
                .download { contentLength, bytesDownloaded, percentDownloaded ->
                    progressCallBack(contentLength, bytesDownloaded, percentDownloaded)

                }

        }

    }


    private val downloaderFactory: DownloaderFactory by lazy {
        DefaultDownloaderFactory(cacheDataSourceFactory, okHttpClient.dispatcher.executorService)
    }


    /**
     * 支持缓存的DataSource.Factory
     */
    private val cacheDataSourceFactory by lazy {
        //使用自定义的CacheDataSource以支持设置UA
        return@lazy CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(okhttpDataFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setCacheWriteDataSinkFactory(
                CacheDataSink.Factory()
                    .setCache(cache)
                    .setFragmentSize(CacheDataSink.DEFAULT_FRAGMENT_SIZE)
            )
    }

    /**
     * Okhttp DataSource.Factory
     */
    private val okhttpDataFactory by lazy {
        OkHttpDataSource.Factory(okHttpClient)
            .setCacheControl(CacheControl.Builder().maxAge(1, TimeUnit.DAYS).build())
    }

    /**
     * Exoplayer 内置的缓存
     */
    private val cache: Cache by lazy {
        val databaseProvider = StandaloneDatabaseProvider(appCtx)
        return@lazy SimpleCache(
            //Exoplayer的缓存路径
            File(appCtx.externalCacheDir, "exoplayer"),
            //100M的缓存
            LeastRecentlyUsedCacheEvictor((100 * 1024 * 1024).toLong()),
            //记录缓存的数据库
            databaseProvider
        )
    }

    /**
     * 通过kotlin扩展函数+反射实现CacheDataSource.Factory设置默认请求头
     * 需要添加混淆规则 -keepclassmembers class com.google.android.exoplayer2.upstream.cache.CacheDataSource$Factory{upstreamDataSourceFactory;}
     * @param headers
     * @return
     */
    private fun CacheDataSource.Factory.setDefaultRequestProperties(headers: Map<String, String> = mapOf()): CacheDataSource.Factory {
        val declaredField = this.javaClass.getDeclaredField("upstreamDataSourceFactory")
        declaredField.isAccessible = true
        val df = declaredField[this] as DataSource.Factory
        if (df is OkHttpDataSource.Factory) {
            df.setDefaultRequestProperties(headers)
        }
        return this
    }

}