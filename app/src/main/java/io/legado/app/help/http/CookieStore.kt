@file:Suppress("unused")

package io.legado.app.help.http

import android.text.TextUtils
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor
import com.franmontiel.persistentcookiejar.persistence.SerializableCookie
import io.legado.app.App
import io.legado.app.data.entities.Cookie
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.NetworkUtils

object CookieStore : CookiePersistor {

    fun setCookie(url: String, cookie: String?) {
        Coroutine.async {
            val cookieBean = Cookie(NetworkUtils.getSubDomain(url), cookie ?: "")
            App.db.cookieDao().insert(cookieBean)
        }
    }

    fun replaceCookie(url: String, cookie: String) {
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

    fun getCookie(url: String): String {
        val cookieBean = App.db.cookieDao().get(NetworkUtils.getSubDomain(url))
        return cookieBean?.cookie ?: ""
    }

    fun removeCookie(url: String) {
        App.db.cookieDao().delete(NetworkUtils.getSubDomain(url))
    }

    fun cookieToMap(cookie: String): MutableMap<String, String> {
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

    fun mapToCookie(cookieMap: Map<String, String>?): String? {
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

    override fun loadAll(): MutableList<okhttp3.Cookie> {
        val cookies = arrayListOf<okhttp3.Cookie>()
        App.db.cookieDao().getOkHttpCookies().forEach {
            val serializedCookie = it.cookie
            SerializableCookie().decode(serializedCookie)?.let { ck ->
                cookies.add(ck)
            }
        }
        return cookies
    }

    override fun saveAll(cookies: MutableCollection<okhttp3.Cookie>?) {
        val mCookies = arrayListOf<Cookie>()
        cookies?.forEach {
            mCookies.add(Cookie(createCookieKey(it), SerializableCookie().encode(it)))
        }
        App.db.cookieDao().insert(*mCookies.toTypedArray())
    }

    override fun removeAll(cookies: MutableCollection<okhttp3.Cookie>?) {
        cookies?.forEach {
            App.db.cookieDao().delete(createCookieKey(it))
        }
    }

    override fun clear() {
        App.db.cookieDao().deleteOkHttp()
    }

    private fun createCookieKey(cookie: okhttp3.Cookie): String {
        return (if (cookie.secure()) "https" else "http") + "://" + cookie.domain() + cookie.path() + "|" + cookie.name()
    }
}