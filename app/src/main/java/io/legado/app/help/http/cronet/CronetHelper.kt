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
import splitties.init.appCtx
import java.util.concurrent.Executor
import java.util.concurrent.Executors


val executor: Executor by lazy { Executors.newSingleThreadExecutor() }

val cronetEngine: ExperimentalCronetEngine by lazy {

    val builder = ExperimentalCronetEngine.Builder(appCtx)
        .setStoragePath(appCtx.externalCacheDir?.absolutePath)
        .enableHttpCache(HTTP_CACHE_DISK, (1024 * 1024 * 50))
        .enableQuic(true)
        .enablePublicKeyPinningBypassForLocalTrustAnchors(true)
        .enableHttp2(true)
    //Brotli压缩
    builder.enableBrotli(true)
    return@lazy builder.build()

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
        requestBuilder.addHeader(headers.name(index), headers.value(index))
    }

    val requestBody = request.body
    if (requestBody != null) {
        val contentType: MediaType? = requestBody.contentType()
        if (contentType != null) {
            requestBuilder.addHeader("Content-Type", contentType.toString())
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

