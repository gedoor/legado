package io.legado.app.help.http

import io.legado.app.constant.AppConst
import io.legado.app.help.http.api.HttpGetApi
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import retrofit2.Retrofit
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

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

    fun simpleGet(url: String, encode: String? = null): String? {
        NetworkUtils.getBaseUrl(url)?.let { baseUrl ->
            val response = getApiService<HttpGetApi>(baseUrl, encode)
                .get(url, mapOf(Pair(AppConst.UA_NAME, AppConst.userAgent)))
                .execute()
            return response.body()
        }
        return null
    }

    suspend fun simpleGetAsync(url: String, encode: String? = null): String? {
        NetworkUtils.getBaseUrl(url)?.let { baseUrl ->
            val response = getApiService<HttpGetApi>(baseUrl, encode)
                .getAsync(url, mapOf(Pair(AppConst.UA_NAME, AppConst.userAgent)))
            return response.body()
        }
        return null
    }

    suspend fun simpleGetBytesAsync(url: String): ByteArray? {
        NetworkUtils.getBaseUrl(url)?.let { baseUrl ->
            return getByteRetrofit(baseUrl)
                .create(HttpGetApi::class.java)
                .getMapByteAsync(url, mapOf(), mapOf(Pair(AppConst.UA_NAME, AppConst.userAgent)))
                .body()
        }
        return null
    }

    inline fun <reified T> getApiService(
        baseUrl: String,
        encode: String? = null,
        proxy: String? = null
    ): T {
        return if (proxy.isNullOrEmpty()) {
            getRetrofit(baseUrl, encode).create(T::class.java)
        } else {
            getRetrofitWithProxy(baseUrl, encode, proxy).create(T::class.java)
        }
    }

    inline fun <reified T> getBytesApiService(baseUrl: String): T {
        return getByteRetrofit(baseUrl).create(T::class.java)
    }

    fun getRetrofit(baseUrl: String, encode: String? = null): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            //增加返回值为字符串的支持(以实体类返回)
            .addConverterFactory(EncodeConverter(encode))
            .client(client)
            .build()
    }

    fun getRetrofitWithProxy(
        baseUrl: String,
        encode: String? = null,
        proxy: String? = null
    ): Retrofit {
        val r = Regex("(http|socks4|socks5)://(.*):(\\d{2,5})(@.*@.*)?")
        val ms = proxy?.let { r.findAll(it) }
        val group = ms?.first()
        var type = "direct"     //直接连接
        var host = "127.0.0.1"  //代理服务器hostname
        var port = 1080            //代理服务器port
        var username = ""       //代理服务器验证用户名
        var password = ""       //代理服务器验证密码
        if (group != null) {
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
        return Retrofit.Builder().baseUrl(baseUrl)
            //增加返回值为字符串的支持(以实体类返回)
            .addConverterFactory(EncodeConverter(encode))
            .client(builder.build())
            .build()
    }

    fun getByteRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(ByteConverter())
            .client(client)
            .build()
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
