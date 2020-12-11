package io.legado.app.help.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import retrofit2.Retrofit
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Suppress("unused")
object HttpHelper {

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
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor(getHeaderInterceptor())

        builder.build()
    }

    suspend fun awaitResponse(request: Request): Response = suspendCancellableCoroutine { block ->
        val call = client.newCall(request)

        block.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                block.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                block.resume(response)
            }
        })
    }

    inline fun <reified T> getApiService(
        baseUrl: String,
        encode: String? = null,
        proxy: String? = null
    ): T {
        return getRetrofit(baseUrl, encode, proxy).create(T::class.java)
    }

    fun getRetrofit(
        baseUrl: String,
        encode: String? = null,
        proxy: String? = null
    ): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            //增加返回值为字符串的支持(以实体类返回)
            .addConverterFactory(EncodeConverter(encode))
            .client(getProxyClient(proxy))
            .build()
    }

    fun getProxyClient(proxy: String? = null): OkHttpClient {
        if (proxy.isNullOrBlank()) {
            return client
        }
        val r = Regex("(http|socks4|socks5)://(.*):(\\d{2,5})(@.*@.*)?")
        val ms = r.findAll(proxy)
        val group = ms.first()
        val type: String     //直接连接
        val host: String  //代理服务器hostname
        val port: Int            //代理服务器port
        var username = ""       //代理服务器验证用户名
        var password = ""       //代理服务器验证密码
        type = if (group.groupValues[1] == "http") {
            "http"
        } else {
            "socks"
        }
        host = group.groupValues[2]
        port = group.groupValues[3].toInt()
        if (group.groupValues[4] != "") {
            username = group.groupValues[4].split("@")[1]
            password = group.groupValues[4].split("@")[2]
        }
        val builder = client.newBuilder()
        if (type != "direct" && host != "") {
            if (type == "http") {
                builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
            } else {
                builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port)))
            }
            if (username != "" && password != "") {
                builder.proxyAuthenticator { _, response -> //设置代理服务器账号密码
                    val credential: String = Credentials.basic(username, password)
                    response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build()
                }
            }

        }
        return builder.build()
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

    suspend fun ajax(params: AjaxWebView.AjaxParams): Res =
        suspendCancellableCoroutine { block ->
            val webView = AjaxWebView()
            block.invokeOnCancellation {
                webView.destroyWebView()
            }
            webView.callback = object : AjaxWebView.Callback() {
                override fun onResult(response: Res) {
                    if (!block.isCompleted)
                        block.resume(response)
                }

                override fun onError(error: Throwable) {
                    if (!block.isCompleted)
                        block.resume(Res(params.url, error.localizedMessage))
                }
            }
            webView.load(params)
        }

}
