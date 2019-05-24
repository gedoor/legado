package io.legado.app.help.http

import okhttp3.*
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.TimeUnit

object HttpHelper {

    val client: OkHttpClient = getOkHttpClient()


    fun <T> getApiService(baseUrl: String, clazz: Class<T>): T {
        return getRetrofit(baseUrl).create(clazz)
    }

    fun getRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            //增加返回值为字符串的支持(以实体类返回)
//            .addConverterFactory(EncodeConverter.create())
            //增加返回值为Observable<T>的支持
            .addCallAdapterFactory(CoroutinesCallAdapterFactory.invoke())
            .client(client)
            .build()
    }

    private fun getOkHttpClient(): OkHttpClient {
        val cs = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()

        val specs = ArrayList<ConnectionSpec>()
        specs.add(cs)
        specs.add(ConnectionSpec.COMPATIBLE_TLS)
        specs.add(ConnectionSpec.CLEARTEXT)

        val sslParams = SSLHelper.getSslSocketFactory()
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
            .hostnameVerifier(SSLHelper.unsafeHostnameVerifier)
            .connectionSpecs(specs)
            .followRedirects(true)
            .followSslRedirects(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor(getHeaderInterceptor())
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
}