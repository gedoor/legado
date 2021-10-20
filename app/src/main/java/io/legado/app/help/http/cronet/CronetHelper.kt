package io.legado.app.help.http.cronet

import com.google.android.gms.net.CronetProviderInstaller
import io.legado.app.help.AppConfig
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okio.Buffer
import org.chromium.net.CronetEngine.Builder.HTTP_CACHE_DISK
import org.chromium.net.ExperimentalCronetEngine
import org.chromium.net.UploadDataProviders
import org.chromium.net.UrlRequest
import splitties.init.appCtx
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


val executor: ExecutorService by lazy { Executors.newCachedThreadPool() }

val cronetEngine: ExperimentalCronetEngine? by lazy {
    if (AppConfig.isGooglePlay) {
        CronetProviderInstaller.installProvider(appCtx)
    } else {
        CronetLoader.preDownload()
    }
    val builder = ExperimentalCronetEngine.Builder(appCtx).apply {
        if (!AppConfig.isGooglePlay && CronetLoader.install()) {
            setLibraryLoader(CronetLoader)//设置自定义so库加载
        }
        setStoragePath(appCtx.externalCacheDir?.absolutePath)//设置缓存路径
        enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50).toLong())//设置50M的磁盘缓存
        enableQuic(true)//设置支持http/3
        enableHttp2(true)  //设置支持http/2
        enablePublicKeyPinningBypassForLocalTrustAnchors(true)
        enableBrotli(true)//Brotli压缩
    }
    try {
        val engine = builder.build()
        Timber.d("Cronet Version:" + engine.versionString)
        return@lazy engine
    } catch (e: Exception) {
        Timber.e(e, "初始化cronetEngine出错")
        return@lazy null
    }
}

fun buildRequest(request: Request, callback: UrlRequest.Callback): UrlRequest? {
    val url = request.url.toString()
    val headers: Headers = request.headers
    val requestBody = request.body
    return cronetEngine?.newUrlRequestBuilder(url, callback, executor)?.apply {
        setHttpMethod(request.method)//设置
        allowDirectExecutor()
        headers.forEachIndexed { index, _ ->
            addHeader(headers.name(index), headers.value(index))
        }
        if (requestBody != null) {
            val contentType: MediaType? = requestBody.contentType()
            if (contentType != null) {
                addHeader("Content-Type", contentType.toString())
            } else {
                addHeader("Content-Type", "text/plain")
            }
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            setUploadDataProvider(
                UploadDataProviders.create(buffer.readByteArray()),
                executor
            )

        }

    }?.build()

}

