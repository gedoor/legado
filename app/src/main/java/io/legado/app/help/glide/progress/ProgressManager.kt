package io.legado.app.help.glide.progress

import android.text.TextUtils
import io.legado.app.constant.AppConst
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.CookieManager
import io.legado.app.help.http.CookieManager.cookieJarHeader
import io.legado.app.help.http.Cronet
import io.legado.app.help.http.DecompressInterceptor
import io.legado.app.help.http.OkHttpExceptionInterceptor
import io.legado.app.help.http.OkhttpUncaughtExceptionHandler
import io.legado.app.help.http.SSLHelper
import io.legado.app.help.http.cloudflare.AndroidCookieJar
import io.legado.app.help.http.cloudflare.CloudflareInterceptor
import io.legado.app.model.ReadMange
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.ImageUtils
import kotlinx.coroutines.runBlocking
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import splitties.init.appCtx
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 进度监听器管理类
 * 加入图片加载进度监听，加入Https支持
 */
object ProgressManager {
    private val listenersMap = ConcurrentHashMap<String, OnProgressListener>()
    val cookieJar = AndroidCookieJar()

    /**glide 下载进度的主要逻辑 需要在GlideModule注入*/
    fun glideProgressInterceptor(): OkHttpClient {
        val specs = arrayListOf(
            ConnectionSpec.MODERN_TLS,
            ConnectionSpec.COMPATIBLE_TLS,
            ConnectionSpec.CLEARTEXT
        )
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .sslSocketFactory(SSLHelper.unsafeSSLSocketFactory, SSLHelper.unsafeTrustManager)
            .retryOnConnectionFailure(true)
            .hostnameVerifier(SSLHelper.unsafeHostnameVerifier)
            .connectionSpecs(specs)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(OkHttpExceptionInterceptor)
            .addInterceptor(CloudflareInterceptor(appCtx, cookieJar, AppConfig::userAgent))
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                if (request.header(AppConst.UA_NAME) == null) {
                    builder.addHeader(AppConst.UA_NAME, AppConfig.userAgent)
                } else if (request.header(AppConst.UA_NAME) == "null") {
                    builder.removeHeader(AppConst.UA_NAME)
                }
                builder.addHeader("Keep-Alive", "300")
                builder.addHeader("Connection", "Keep-Alive")
                builder.addHeader("Cache-Control", "no-cache")
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
                            .body(modifiedBytes?.toResponseBody(res.body?.contentType()))
                            .build()
                    } else {
                        chain.proceed(builder.build())
                    }
                }
            })
            .addNetworkInterceptor { chain ->
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
        if (AppConfig.isCronet) {
            if (Cronet.loader?.install() == true) {
                Cronet.interceptor?.let {
                    builder.addInterceptor(it)
                }
            }
        }
        builder.addInterceptor(DecompressInterceptor)
        return builder.build().apply {
            val okHttpName =
                OkHttpClient::class.java.name.removePrefix("okhttp3.").removeSuffix("Client")
            val executor = dispatcher.executorService as ThreadPoolExecutor
            val threadName = "$okHttpName Dispatcher"
            executor.threadFactory = ThreadFactory { runnable ->
                Thread(runnable, threadName).apply {
                    isDaemon = false
                    uncaughtExceptionHandler = OkhttpUncaughtExceptionHandler
                }
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
                it.invoke(isComplete, percentage, bytesRead, totalBytes)
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