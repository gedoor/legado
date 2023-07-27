@file:Suppress("UnusedReceiverParameter")

package io.legado.app.utils

import android.webkit.CookieManager


@Suppress("unused")
fun CookieManager.removeCookie(domain: String) {
    val cm = CookieManager.getInstance()
    val urls = arrayOf(
        "http://$domain",
        "https://$domain"
    )
    urls.forEach { url ->
        val cookieGlob: String? = cm.getCookie(url)
        cookieGlob?.splitNotBlank(";")?.forEach {
            val cookieName = it.substringBefore("=")
            cm.setCookie(url, "$cookieName=; Expires=Wed, 31 Dec 2000 23:59:59 GMT")
        }
    }
}