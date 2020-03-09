package io.legado.app.help.http

import io.legado.app.help.http.api.HttpGetApi
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

@Suppress("unused")
object HttpHelper {

    val client: OkHttpClient by lazy {
        val default = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()

        val specs = ArrayList<ConnectionSpec>()
        specs.add(default)
        specs.add(ConnectionSpec.COMPATIBLE_TLS)
        specs.add(ConnectionSpec.CLEARTEXT)

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
                .get(url, mapOf())
                .execute()
            return response.body()
        }
        return null
    }

    suspend fun simpleGetAsync(url: String, encode: String? = null): String? {
        NetworkUtils.getBaseUrl(url)?.let { baseUrl ->
            val response = getApiService<HttpGetApi>(baseUrl, encode)
                .getAsync(url, mapOf())
            return response.body()
        }
        return null
    }

    suspend fun simpleGetByteAsync(url: String): ByteArray? {
        NetworkUtils.getBaseUrl(url)?.let { baseUrl ->
            return getByteRetrofit(baseUrl)
                .create(HttpGetApi::class.java)
                .getMapByteAsync(url, mapOf(), mapOf())
                .body()
        }
        return null
    }

    inline fun <reified T> getApiService(baseUrl: String, encode: String? = null): T {
        return getRetrofit(baseUrl, encode).create(T::class.java)
    }

    fun getRetrofit(baseUrl: String, encode: String? = null): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            //增加返回值为字符串的支持(以实体类返回)
            .addConverterFactory(EncodeConverter(encode))
            .client(client)
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