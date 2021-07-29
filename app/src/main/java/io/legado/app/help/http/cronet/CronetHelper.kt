package io.legado.app.help.http.cronet

import io.legado.app.help.http.CookieStore
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okio.Buffer
import org.chromium.net.CronetEngine.Builder.HTTP_CACHE_DISK
import org.chromium.net.ExperimentalCronetEngine
import org.chromium.net.UploadDataProviders
import org.chromium.net.UrlRequest
import org.chromium.net.urlconnection.CronetURLStreamHandlerFactory
import splitties.init.appCtx
import java.net.URL
import java.util.concurrent.Executor
import java.util.concurrent.Executors


val executor: Executor by lazy { Executors.newSingleThreadExecutor() }

val cronetEngine: ExperimentalCronetEngine by lazy {
    CronetLoader.preDownload()

    val builder = ExperimentalCronetEngine.Builder(appCtx)
        //设置自定义so库加载
        .setLibraryLoader(CronetLoader)
        //设置缓存路径
        .setStoragePath(appCtx.externalCacheDir?.absolutePath)
        //设置缓存模式
        .enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50))
        //设置支持http/3
        .enableQuic(true)
        //设置支持http/2
        .enableHttp2(true)
        .enablePublicKeyPinningBypassForLocalTrustAnchors(true)
    //.enableNetworkQualityEstimator(true)

    //Brotli压缩
    builder.enableBrotli(true)
    //builder.setExperimentalOptions("{\"quic_version\": \"h3-29\"}")
    val engine = builder.build()
    URL.setURLStreamHandlerFactory(CronetURLStreamHandlerFactory(engine))
//    engine.addRequestFinishedListener(object : RequestFinishedInfo.Listener(executor) {
//        override fun onRequestFinished(requestFinishedInfo: RequestFinishedInfo?) {
//            val sb = StringBuilder(requestFinishedInfo!!.url).append("\r\n")
//
//            try {
//                if (requestFinishedInfo.responseInfo != null) {
//                    val responseInfo = requestFinishedInfo.responseInfo
//                    if (responseInfo != null) {
//                        sb.append("[Cached:").append(responseInfo.wasCached())
//                            .append("][StatusCode:")
//                            .append(
//                                responseInfo.httpStatusCode
//                            ).append("][StatusText:").append(responseInfo.httpStatusText)
//                            .append("][Protocol:").append(responseInfo.negotiatedProtocol)
//                            .append("][ByteCount:").append(
//                                responseInfo.receivedByteCount
//                            ).append("]\r\n")
//                    }
//                    val httpHeaders = requestFinishedInfo.responseInfo!!
//                        .allHeadersAsList
//                    for ((key, value) in httpHeaders) {
//                        sb.append("[").append(key).append("]").append(value).append("\r\n")
//                    }
//                    Log.e("Cronet", sb.toString())
//                }
//            } catch (e: URISyntaxException) {
//                e.printStackTrace()
//            }
//        }
//    })
    return@lazy engine

}


fun buildRequest(request: Request, callback: UrlRequest.Callback): UrlRequest {
    val url = request.url.toString()
    val requestBuilder = cronetEngine.newUrlRequestBuilder(url, callback, executor)
    requestBuilder.setHttpMethod(request.method)
    val cookie = CookieStore.getCookie(url)
    if (cookie.length > 1) {
        requestBuilder.addHeader("Cookie", cookie)
    }
    val headers: Headers = request.headers
    headers.forEachIndexed { index, _ ->
        val name = headers.name(index)
        if (!name.equals("Keep-Alive", true)) {
            requestBuilder.addHeader(name, headers.value(index))
        }

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

