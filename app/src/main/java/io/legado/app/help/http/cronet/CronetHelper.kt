package io.legado.app.help.http.cronet

import android.util.Log
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okio.Buffer
import org.chromium.net.CronetEngine.Builder.HTTP_CACHE_DISK
import org.chromium.net.ExperimentalCronetEngine
import org.chromium.net.UploadDataProviders
import org.chromium.net.UrlRequest
import splitties.init.appCtx
import java.util.concurrent.Executor
import java.util.concurrent.Executors


val executor: Executor by lazy { Executors.newCachedThreadPool() }

val cronetEngine: ExperimentalCronetEngine by lazy {
    CronetLoader.preDownload()

    val builder = ExperimentalCronetEngine.Builder(appCtx)
        .setLibraryLoader(CronetLoader)//设置自定义so库加载
        .setStoragePath(appCtx.externalCacheDir?.absolutePath)//设置缓存路径
        .enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50))//设置缓存模式
        .enableQuic(true)//设置支持http/3
        .enableHttp2(true)  //设置支持http/2
        .enablePublicKeyPinningBypassForLocalTrustAnchors(true)
    //.enableNetworkQualityEstimator(true)

    //Brotli压缩
    builder.enableBrotli(true)
    //builder.setExperimentalOptions("{\"quic_version\": \"h3-29\"}")
    val engine = builder.build()
    Log.d("Cronet", "Cronet Version:" + engine.versionString)
    //这会导致Jsoup的网络请求出现问题，暂时不接管系统URL
    //URL.setURLStreamHandlerFactory(CronetURLStreamHandlerFactory(engine))
    return@lazy engine

}


fun buildRequest(request: Request, callback: UrlRequest.Callback): UrlRequest {
    val url = request.url.toString()
    val requestBuilder = cronetEngine.newUrlRequestBuilder(url, callback, executor)
    requestBuilder.setHttpMethod(request.method)

    val headers: Headers = request.headers
    headers.forEachIndexed { index, _ ->
        requestBuilder.addHeader(headers.name(index), headers.value(index))
    }

    val requestBody = request.body
    if (requestBody != null) {
        val contentType: MediaType? = requestBody.contentType()
        if (contentType != null) {
            requestBuilder.addHeader("Content-Type", contentType.toString())
        } else {
            requestBuilder.addHeader("Content-Type", "text/plain")
        }
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        requestBuilder.setUploadDataProvider(
            UploadDataProviders.create(buffer.readByteArray()),
            executor
        )

    }

    return requestBuilder.build()
}

