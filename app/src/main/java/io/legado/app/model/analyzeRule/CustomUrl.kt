package io.legado.app.model.analyzeRule

import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

@Suppress("unused")
class CustomUrl(url: String) {

    private val mUrl: String
    private val attribute = hashMapOf<String, Any>()

    init {
        val urlMatcher = AnalyzeUrl.paramPattern.matcher(url)
        mUrl = if (urlMatcher.find()) {
            val attr = url.substring(urlMatcher.end())
            GSON.fromJsonObject<Map<String, Any>>(attr).getOrNull()?.let {
                attribute.putAll(it)
            }
            url.substring(0, urlMatcher.start())
        } else {
            url
        }
    }

    fun putAttribute(key: String, value: Any?): CustomUrl {
        if (value == null) {
            attribute.remove(key)
        } else {
            attribute[key] = value
        }
        return this
    }

    fun getUrl(): String {
        return mUrl
    }

    fun getAttr(): Map<String, Any> {
        return attribute
    }

    override fun toString(): String {
        if (attribute.isEmpty()) {
            return mUrl
        }
        return mUrl + "," + GSON.toJson(attribute)
    }

}