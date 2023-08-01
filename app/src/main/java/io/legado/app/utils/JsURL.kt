package io.legado.app.utils

import androidx.annotation.Keep
import java.net.URL
import java.net.URLDecoder

@Keep
@Suppress("MemberVisibilityCanBePrivate")
class JsURL(url: String, baseUrl: String? = null) {

    val searchParams: Map<String, String>?
    val host: String
    val origin: String
    val pathname: String

    init {
        val mUrl = if (!baseUrl.isNullOrEmpty()) {
            val base = URL(baseUrl)
            URL(base, url)
        } else {
            URL(url)
        }
        host = mUrl.host
        origin = if (mUrl.port > 0) {
            "${mUrl.protocol}://$host:${mUrl}:${mUrl.port}"
        } else {
            "${mUrl.protocol}://$host:${mUrl}"
        }
        pathname = mUrl.path
        val query = mUrl.query
        searchParams = query?.let { _ ->
            val map = hashMapOf<String, String>()
            query.split("&").forEach {
                val x = it.split("=")
                map[x[0]] = URLDecoder.decode(x[1], "utf-8")
            }
            map
        }
    }


}