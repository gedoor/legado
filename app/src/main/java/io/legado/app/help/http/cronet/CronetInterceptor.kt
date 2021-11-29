package io.legado.app.help.http.cronet

import io.legado.app.help.http.CookieStore
import okhttp3.*
import java.io.IOException

class CronetInterceptor(private val cookieJar: CookieJar?) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original: Request = chain.request()
        //Cronet未初始化
        return if (!CronetLoader.install() || cronetEngine == null) {
            chain.proceed(original)
        } else try {
            val builder: Request.Builder = original.newBuilder()
            //移除Keep-Alive,手动设置会导致400 BadRequest
            builder.removeHeader("Keep-Alive")
            builder.removeHeader("Accept-Encoding")
            val cookieStr = getCookie(original.url)
            //设置Cookie
            if (cookieStr.length > 3) {
                builder.header("Cookie", cookieStr)
            }
            val new = builder.build()
            proceedWithCronet(new, chain.call())?.let { response ->
                //从Response 中保存Cookie到CookieJar
                cookieJar?.saveFromResponse(new.url, Cookie.parseAll(new.url, response.headers))
                response
            } ?: chain.proceed(original)
        } catch (e: Exception) {
            //遇到Cronet处理有问题时的情况，如证书过期等等，回退到okhttp处理
            chain.proceed(original)
        }

    }

    @Throws(IOException::class)
    private fun proceedWithCronet(request: Request, call: Call): Response? {
        val callback = CronetRequestCallback(request, call)
        buildRequest(request, callback)?.let {
            it.start()
            return callback.waitForDone(it)
        }
        return null
    }

    private fun getCookie(url: HttpUrl): String {
        val sb = StringBuilder()
        //处理从 Cookiejar 获取到的Cookies
        if (cookieJar != null) {
            val cookies = cookieJar.loadForRequest(url)
            for (cookie in cookies) {
                sb.append(cookie.name).append("=").append(cookie.value).append("; ")
            }
        }
        //处理自定义的Cookie
        val cookie = CookieStore.getCookie(url.toString())
        if (cookie.length > 3) {
            sb.append(cookie)
        }
        return sb.toString()
    }

}
