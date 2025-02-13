package io.legado.app.help.glide.progress

import android.text.TextUtils
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.CookieManager
import io.legado.app.help.http.CookieManager.cookieJarHeader
import io.legado.app.help.http.cloudflare.AndroidCookieJar
import io.legado.app.help.http.cloudflare.CloudflareInterceptor
import io.legado.app.help.http.getOkHttpClient
import io.legado.app.model.ReadMange
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.ImageUtils
import io.legado.app.utils.runOnUI
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import splitties.init.appCtx
import java.util.concurrent.ConcurrentHashMap

/**
 * 进度监听器管理类
 * 加入图片加载进度监听，加入Https支持
 */
object ProgressManager {
    private val listenersMap = ConcurrentHashMap<String, OnProgressListener>()
    val cookieJar = AndroidCookieJar()

    /**glide 下载进度的主要逻辑 需要在GlideModule注入*/
    fun glideProgressInterceptor(): OkHttpClient {
        return getOkHttpClient {
            addInterceptor(CloudflareInterceptor(appCtx, cookieJar, AppConfig::userAgent))
            addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                runBlocking {
                    if (ReadMange.isMangaLookModel) {
                        val analyzeUrl = AnalyzeUrl(
                            request.url.toString(),
                            source = ReadMange.bookSource
                        )
                        val bytes = analyzeUrl.getByteArrayAwait()
                        val modifiedBytes = ImageUtils.decode(
                            request.url.toString(),
                            bytes,
                            isCover = false,
                            ReadMange.bookSource,
                            ReadMange.book
                        )
                        val res = chain.proceed(request)
                        res.newBuilder()
                            .body(
                                ProgressResponseBody(
                                    request.url.toString(),
                                    LISTENER,
                                    modifiedBytes?.toResponseBody(res.body?.contentType())!!
                                )
                            )
                            .build()
                    } else {
                        chain.proceed(builder.build())
                    }
                }
            })

            addNetworkInterceptor { chain ->
                var request = chain.request()
                val enableCookieJar = request.header(cookieJarHeader) != null

                if (enableCookieJar) {
                    val requestBuilder = request.newBuilder()
                    requestBuilder.removeHeader(cookieJarHeader)
                    request = CookieManager.loadRequest(requestBuilder.build())
                }

                val networkResponse = chain.proceed(request)

                if (enableCookieJar) {
                    CookieManager.saveResponse(networkResponse)
                }
                networkResponse.newBuilder()
                    .body(
                        ProgressResponseBody(
                            request.url.toString(),
                            LISTENER,
                            networkResponse.body!!
                        )
                    )
                    .build()
            }
        }
    }

    val LISTENER = object : ProgressResponseBody.InternalProgressListener {
        override fun onProgress(url: String, bytesRead: Long, totalBytes: Long) {
            getProgressListener(url)?.let {
                var percentage = (bytesRead * 1f / totalBytes * 100f).toInt()
                var isComplete = percentage >= 100
                if (percentage <= -100) {
                    percentage = 0
                    isComplete = true
                }
                runOnUI {
                    it.invoke(isComplete, percentage, bytesRead, totalBytes)
                }
                if (isComplete) {
                    removeListener(url)
                }
            }
        }
    }

    fun addListener(url: String, listener: OnProgressListener) {
        if (!TextUtils.isEmpty(url) && listener != null) {
            listenersMap[url] = listener
            listener.invoke(false, 1, 0, 0)
        }
    }

    fun removeListener(url: String) {
        if (!TextUtils.isEmpty(url)) {
            listenersMap.remove(url)
        }
    }

    fun getProgressListener(url: String?): OnProgressListener {
        return if (TextUtils.isEmpty(url) || listenersMap.size == 0) {
            null
        } else listenersMap[url]
    }
}