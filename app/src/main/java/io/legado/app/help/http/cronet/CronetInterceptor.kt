package io.legado.app.help.http.cronet

import io.legado.app.help.http.CookieStore
import okhttp3.*
import java.io.IOException

class CronetInterceptor(private val cookieJar: CookieJar?) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original: Request = chain.request()
        val builder: Request.Builder = original.newBuilder()
        //Cronet未初始化
        return if (!CronetLoader.install()) {
            chain.proceed(original)
        } else try {
            //移除Keep-Alive,手动设置会导致400 BadRequest
            builder.removeHeader("Keep-Alive")
            val cookieStr = getCookie(original.url)
            //设置Cookie
            if (cookieStr.length > 3) {
                builder.header("Cookie", cookieStr)
            }
            val new = builder.build()
            val response: Response = proceedWithCronet(new, chain.call())
            //从Response 中保存Cookie到CookieJar
            cookieJar?.saveFromResponse(new.url, Cookie.parseAll(new.url, response.headers))
            response
        } catch (e: Exception) {
            //遇到Cronet处理有问题时的情况，如证书过期等等，回退到okhttp处理
            chain.proceed(original)
        }


    }

    @Throws(IOException::class)
    private fun proceedWithCronet(request: Request, call: Call): Response {

        val callback = CronetUrlRequestCallback(request, call)
        val urlRequest = buildRequest(request, callback)
        urlRequest.start()
        return callback.waitForDone()
    }

    private fun getCookie(url: HttpUrl): String {
        val sb = StringBuilder()
        //处理从 Cookjar 获取到的Cookies
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