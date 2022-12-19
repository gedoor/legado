package io.legado.app.help.http.cronet

import io.legado.app.constant.AppLog
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.DebugLog
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import org.chromium.net.CronetEngine.Builder.HTTP_CACHE_DISK
import org.chromium.net.ExperimentalCronetEngine
import org.chromium.net.UrlRequest
import org.json.JSONObject
import splitties.init.appCtx


val cronetEngine: ExperimentalCronetEngine? by lazy {
    if (!AppConfig.isGooglePlay) {
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
        setExperimentalOptions(options)
    }
    try {
        val engine = builder.build()
        DebugLog.d("Cronet Version:", engine.versionString)
        return@lazy engine
    } catch (e: UnsatisfiedLinkError) {
        AppLog.put("初始化cronetEngine出错", e)
        return@lazy null
    }
}

val options by lazy {
    val options = JSONObject()

    //设置域名映射规则
    //MAP hostname ip,MAP hostname ip
//    val host = JSONObject()
//    host.put("host_resolver_rules","")
//    options.put("HostResolverRules", host)

    //启用DnsHttpsSvcb更容易迁移到http3
    val dnsSvcb = JSONObject()
    dnsSvcb.put("enable", true)
    dnsSvcb.put("enable_insecure", true)
    dnsSvcb.put("use_alpn", true)
    options.put("UseDnsHttpsSvcb", dnsSvcb)

    options.put("AsyncDNS", JSONObject("{'enable':true}"))


    options.toString()
}

fun buildRequest(request: Request, callback: UrlRequest.Callback): UrlRequest? {
    val url = request.url.toString()
    val headers: Headers = request.headers
    val requestBody = request.body
    return cronetEngine?.newUrlRequestBuilder(
        url,
        callback,
        okHttpClient.dispatcher.executorService
    )?.apply {
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
            setUploadDataProvider(
                BodyUploadProvider(requestBody),
                okHttpClient.dispatcher.executorService
            )

        }

    }?.build()

}

