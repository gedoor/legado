package io.legado.app.model.analyzeRule

import android.text.TextUtils.isEmpty
import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getPrefString
import java.util.*

/**
 * Created by GKF on 2018/3/2.
 * 解析Headers
 */

object AnalyzeHeaders {

    private val defaultUserAgent: String
        get() = App.INSTANCE.getPrefString("user_agent")
            ?: App.INSTANCE.getString(R.string.pv_user_agent)

    fun getMap(bookSource: BookSource?): Map<String, String> {
        val headerMap = HashMap<String, String>()
        if (bookSource != null && !isEmpty(bookSource.header)) {
            bookSource.header?.let {
                val map: HashMap<String, String>? = GSON.fromJsonObject<HashMap<String, String>>(it)
                map?.let { headerMap.putAll(map) }
            }
        }
        if (bookSource != null) {
            val cookie = App.db.sourceCookieDao().getCookieByUrl(bookSource.bookSourceUrl)
            cookie?.let { headerMap["Cookie"] = cookie }
        }
        return headerMap
    }
}
