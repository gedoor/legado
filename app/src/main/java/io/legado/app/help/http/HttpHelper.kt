package io.legado.app.help.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ConnectionSpec
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

@Suppress("unused")
object HttpHelper {

    private val proxyClientCache: ConcurrentHashMap<String, OkHttpClient> by lazy {
        ConcurrentHashMap()
    }

    val client: OkHttpClient by lazy {

        val specs = arrayListOf(
            ConnectionSpec.MODERN_TLS,
            ConnectionSpec.COMPATIBLE_TLS,
            ConnectionSpec.CLEARTEXT
        )

        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .sslSocketFactory(SSLHelper.unsafeSSLSocketFactory, SSLHelper.unsafeTrustManager)
            .retryOnConnectionFailure(true)
            .hostnameVerifier(SSLHelper.unsafeHostnameVerifier)
            .connectionSpecs(specs)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(getHeaderInterceptor())

        builder.build()
    }

    /**
     * 缓存代理okHttp
     */
    fun getProxyClient(proxy: String? = null): OkHttpClient {
        if (proxy.isNullOrBlank()) {
            return client
        }
        proxyClientCache[proxy]?.let {
            return it
        }
        val r = Regex("(http|socks4|socks5)://(.*):(\\d{2,5})(@.*@.*)?")
        val ms = r.findAll(proxy)
        val group = ms.first()
        var username = ""       //代理服务器验证用户名
        var password = ""       //代理服务器验证密码
        val type = if (group.groupValues[1] == "http") "http" else "socks"
        val host = group.groupValues[2]
        val port = group.groupValues[3].toInt()
        if (group.groupValues[4] != "") {
            username = group.groupValues[4].split("@")[1]
            password = group.groupValues[4].split("@")[2]
        }
        if (type != "direct" && host != "") {
            val builder = client.newBuilder()
            if (type == "http") {
                builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            } else {
                builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port)))
            }
            if (username != "" && password != "") {
                builder.proxyAuthenticator { _, response -> //设置代理服务器账号密码
                    val credential: String = Credentials.basic(username, password)
                    response.request.newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            }
            val proxyClient = builder.build()
            proxyClientCache[proxy] = proxyClient
            return proxyClient
        }
        return client
    }

    private fun getHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader("Keep-Alive", "300")
                .addHeader("Connection", "Keep-Alive")
                .addHeader("Cache-Control", "no-cache")
                .build()
            chain.proceed(request)
        }
    }

    suspend fun ajax(params: AjaxWebView.AjaxParams): StrResponse =
        suspendCancellableCoroutine { block ->
            val webView = AjaxWebView()
            block.invokeOnCancellation {
                webView.destroyWebView()
            }
            webView.callback = object : AjaxWebView.Callback() {
                override fun onResult(response: StrResponse) {

                    if (!block.isCompleted)
                        block.resume(response)
                }

                override fun onError(error: Throwable) {
                    if (!block.isCompleted)
                        block.cancel(error)
                }
            }
            webView.load(params)
        }

}
