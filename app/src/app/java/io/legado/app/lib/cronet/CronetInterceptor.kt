package io.legado.app.lib.cronet

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep
import io.legado.app.utils.printOnDebug
import okhttp3.*
import java.io.IOException

@Keep
@Suppress("unused")
class CronetInterceptor(private val cookieJar: CookieJar) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.call().isCanceled()) {
            throw IOException("Canceled")
        }
        val original: Request = chain.request()
        //Cronet未初始化
        return if (!CronetLoader.install() || cronetEngine == null) {
            chain.proceed(original)
        } else try {
            val builder: Request.Builder = original.newBuilder()
            //移除Keep-Alive,手动设置会导致400 BadRequest
            builder.removeHeader("Keep-Alive")
            builder.removeHeader("Accept-Encoding")

            val newReq = builder.build()
            proceedWithCronet(newReq, chain.call())?.let { response ->
                //从Response 中保存Cookie到CookieJar
                //cookieJar.receiveHeaders(newReq.url, response.headers)
                response
            } ?: chain.proceed(original)
        } catch (e: Exception) {
            //不能抛出错误,抛出错误会导致应用崩溃
            //遇到Cronet处理有问题时的情况，如证书过期等等，回退到okhttp处理
            if (!e.message.toString().contains("ERR_CERT_", true)
                && !e.message.toString().contains("ERR_SSL_", true)
            ) {
                e.printOnDebug()
            }
            chain.proceed(original)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun proceedWithCronet(request: Request, call: Call): Response? {
        val callBack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NewCallBack(request, call)
        } else {
            OldCallback(request, call)
        }
        buildRequest(request, callBack)?.runCatching {
            return callBack.waitForDone(this)
        }
        return null
    }


    /** Returns a 'Cookie' HTTP request header with all cookies, like `a=b; c=d`. */
    private fun getCookie(url: HttpUrl): String = buildString {
        val cookies = cookieJar.loadForRequest(url)
        cookies.forEachIndexed { index, cookie ->
            if (index > 0) append("; ")
            append(cookie.name).append('=').append(cookie.value)
        }
    }

}
