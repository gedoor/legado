package io.legado.app.utils

import java.net.URL
import java.net.URLDecoder

@Suppress("MemberVisibilityCanBePrivate")
class JsURL(url: String, baseUrl: String? = null) {

    val searchParams: Map<String, String>?

    init {
        val mUrl = if (!baseUrl.isNullOrEmpty()) {
            val base = URL(baseUrl)
            URL(base, url)
        } else {
            URL(url)
        }
        val query = mUrl.query
        searchParams = query?.let { query ->
            val map = hashMapOf<String, String>()
            query.split("&").forEach {
                val x = it.split("=")
                map[x[0]] = URLDecoder.decode(x[1], "utf-8")
            }
            map
        }
    }


}