package io.legado.app.help.http.api

interface CookieManager {

    /**
     * 保存cookie
     */
    fun setCookie(url: String, cookie: String?)

    /**
     * 替换cookie
     */
    fun replaceCookie(url: String, cookie: String)

    /**
     * 获取cookie
     */
    fun getCookie(url: String): String

    /**
     * 移除cookie
     */
    fun removeCookie(url: String)

    fun cookieToMap(cookie: String): MutableMap<String, String>

    fun mapToCookie(cookieMap: Map<String, String>?): String?
}