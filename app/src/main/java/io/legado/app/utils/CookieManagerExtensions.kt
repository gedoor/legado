@file:Suppress("UnusedReceiverParameter")

package io.legado.app.utils

import android.webkit.CookieManager


@Suppress("unused")
fun CookieManager.removeCookie(url: String) {
    val cm = CookieManager.getInstance()
    val domains = arrayOf(
        NetworkUtils.getDomain(url),
        NetworkUtils.getSubDomain(url)
    )
    domains.forEach { dm ->
        val cookieGlob: String? = cm.getCookie(dm)
        cookieGlob?.splitNotBlank(";")?.forEach {
            val cookieName = it.substringBefore("=")
            cm.setCookie(dm, "$cookieName=; Expires=Wed, 31 Dec 2000 23:59:59 GMT")
        }
    }
}