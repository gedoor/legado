@file:Suppress("unused")

package io.legado.app.help.http

import android.text.TextUtils
import io.legado.app.App
import io.legado.app.data.entities.Cookie
import io.legado.app.help.http.api.CookieManager
import io.legado.app.utils.NetworkUtils

object CookieStore : CookieManager {

    override fun setCookie(url: String, cookie: String?) {
        val cookieBean = Cookie(NetworkUtils.getSubDomain(url), cookie ?: "")
        App.db.cookieDao.insert(cookieBean)
    }

    override fun replaceCookie(url: String, cookie: String) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(cookie)) {
            return
        }
        val oldCookie = getCookie(url)
        if (TextUtils.isEmpty(oldCookie)) {
            setCookie(url, cookie)
        } else {
            val cookieMap = cookieToMap(oldCookie)
            cookieMap.putAll(cookieToMap(cookie))
            val newCookie = mapToCookie(cookieMap)
            setCookie(url, newCookie)
        }
    }

    override fun getCookie(url: String): String {
        val cookieBean = App.db.cookieDao.get(NetworkUtils.getSubDomain(url))
        return cookieBean?.cookie ?: ""
    }

    override fun removeCookie(url: String) {
        App.db.cookieDao.delete(NetworkUtils.getSubDomain(url))
    }

    override fun cookieToMap(cookie: String): MutableMap<String, String> {
        val cookieMap = mutableMapOf<String, String>()
        if (cookie.isBlank()) {
            return cookieMap
        }
        val pairArray = cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pair in pairArray) {
            val pairs = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (pairs.size == 1) {
                continue
            }
            val key = pairs[0].trim { it <= ' ' }
            val value = pairs[1]
            if (value.isNotBlank() || value.trim { it <= ' ' } == "null") {
                cookieMap[key] = value.trim { it <= ' ' }
            }
        }
        return cookieMap
    }

    override fun mapToCookie(cookieMap: Map<String, String>?): String? {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return null
        }
        val builder = StringBuilder()
        for (key in cookieMap.keys) {
            val value = cookieMap[key]
            if (value?.isNotBlank() == true) {
                builder.append(key)
                    .append("=")
                    .append(value)
                    .append(";")
            }
        }
        return builder.deleteCharAt(builder.lastIndexOf(";")).toString()
    }

    fun clear() {
        App.db.cookieDao.deleteOkHttp()
    }

}