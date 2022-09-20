package io.legado.app.help.source

import io.legado.app.data.entities.RssSource
import io.legado.app.utils.ACache
import io.legado.app.utils.MD5Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val aCache by lazy { ACache.get("rssSortUrl") }

private fun RssSource.sortUrlsKey(): String {
    return MD5Utils.md5Encode(sourceUrl + sortUrl)
}

suspend fun RssSource.sortUrls(): List<Pair<String, String>> =
    arrayListOf<Pair<String, String>>().apply {
        val sortUrlsKey = sortUrlsKey()
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                var a = sortUrl
                if (sortUrl?.startsWith("<js>", false) == true
                    || sortUrl?.startsWith("@js:", false) == true
                ) {
                    a = aCache.getAsString(sortUrlsKey) ?: ""
                    if (a.isBlank()) {
                        val jsStr = if (sortUrl!!.startsWith("@")) {
                            sortUrl!!.substring(4)
                        } else {
                            sortUrl!!.substring(4, sortUrl!!.lastIndexOf("<"))
                        }
                        a = evalJS(jsStr).toString()
                        aCache.put(sortUrlsKey, a)
                    }
                }
                a?.split("(&&|\n)+".toRegex())?.forEach { c ->
                    val d = c.split("::")
                    if (d.size > 1)
                        add(Pair(d[0], d[1]))
                }
                if (isEmpty()) {
                    add(Pair("", sourceUrl))
                }
            }
        }
    }

suspend fun RssSource.removeSortCache() {
    withContext(Dispatchers.IO) {
        aCache.remove(sortUrlsKey())
    }
}