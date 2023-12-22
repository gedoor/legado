package io.legado.app.help.http

import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.help.CacheManager
import io.legado.app.utils.NetworkUtils
import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Connection

@Suppress("ConstPropertyName")
object CookieManager {
    /**
     * <domain>_session_cookie 会话期 cookie，应用重启后失效
     * <domain>_cookie cookies 缓存
     */

    const val cookieJarHeader = "CookieJar"

    /**
     * 从响应中保存Cookies
     */
    fun saveResponse(response: Response) {
        val url = response.request.url
        val headers = response.headers
        saveCookiesFromHeaders(url, headers)
    }

    fun saveResponse(response: Connection.Response) {
        val url = response.url().toHttpUrlOrNull() ?: return
        val headers = response.multiHeaders().toHeaders()
        saveCookiesFromHeaders(url, headers)
    }

    private fun saveCookiesFromHeaders(url: HttpUrl, headers: Headers) {
        val domain = NetworkUtils.getSubDomain(url.toString())
        val cookies = Cookie.parseAll(url, headers)

        val sessionCookie = cookies.filter { !it.persistent }.getString()
        updateSessionCookie(domain, sessionCookie)

        val cookieString = cookies.filter { it.persistent }.getString()
        CookieStore.replaceCookie(domain, cookieString)
    }

    /**
     * 加载Cookies到请求中
     */
    fun loadRequest(request: Request): Request {
        val url = request.url.toString()
        val domain = NetworkUtils.getSubDomain(url)

        val cookie = CookieStore.getCookie(domain)
        val requestCookie = request.header("Cookie")

        mergeCookies(requestCookie, cookie)?.let { newCookie ->
            kotlin.runCatching {
                return request.newBuilder()
                    .header("Cookie", newCookie)
                    .build()
            }.onFailure {
                CookieStore.removeCookie(url)
                AppLog.put(
                    "设置cookie出错，已清除cookie $domain cookie:$newCookie\n${it.localizedMessage}",
                    it
                )
            }
        }
        return request
    }

    private fun getSessionCookieMap(domain: String): MutableMap<String, String>? {
        return getSessionCookie(domain)?.let { CookieStore.cookieToMap(it) }
    }

    fun getSessionCookie(domain: String): String? {
        return CacheManager.getFromMemory("${domain}_session_cookie") as? String
    }

    private fun updateSessionCookie(domain: String, cookies: String) {
        val sessionCookie = getSessionCookie(domain)
        if (sessionCookie.isNullOrEmpty()) {
            CacheManager.putMemory("${domain}_session_cookie", cookies)
            return
        }

        val ck = mergeCookies(sessionCookie, cookies) ?: return
        CacheManager.putMemory("${domain}_session_cookie", ck)
    }

    fun mergeCookies(vararg cookies: String?): String? {
        val cookieMap = mergeCookiesToMap(*cookies)
        return CookieStore.mapToCookie(cookieMap)
    }

    fun mergeCookiesToMap(vararg cookies: String?): MutableMap<String, String> {
        return cookies.filterNotNull().map {
            CookieStore.cookieToMap(it)
        }.reduce { acc, cookieMap ->
            acc.apply { putAll(cookieMap) }
        }
    }

    /**
     * 删除单个Cookie
     */
    fun removeCookie(url: String, key: String) {
        val domain = NetworkUtils.getSubDomain(url)

        getSessionCookieMap(domain)?.let {
            it.remove(key)
            CookieStore.mapToCookie(it)?.let { cookie ->
                CacheManager.putMemory("${domain}_session_cookie", cookie)
            }
        }

        val cookie = getCookieNoSession(url)
        if (cookie.isNotEmpty()) {
            val cookieMap = CookieStore.cookieToMap(cookie).apply { remove(key) }
            CookieStore.mapToCookie(cookieMap)?.let {
                CookieStore.setCookie(url, it)
            }
        }
    }

    fun getCookieNoSession(url: String): String {
        val domain = NetworkUtils.getSubDomain(url)
        val cacheCookie = CacheManager.getFromMemory("${domain}_cookie") as? String

        return if (cacheCookie != null) {
            cacheCookie
        } else {
            val cookieBean = appDb.cookieDao.get(domain)
            cookieBean?.cookie ?: ""
        }
    }

    fun List<Cookie>.getString() = buildString {
        this@getString.forEachIndexed { index, cookie ->
            if (index > 0) append("; ")
            append(cookie.name).append('=').append(cookie.value)
        }
    }

    private fun Map<String, List<String>>.toHeaders(): Headers {
        return Headers.Builder().apply {
            this@toHeaders.forEach { (k, v) ->
                v.forEach {
                    add(k, it)
                }
            }
        }.build()
    }

}
