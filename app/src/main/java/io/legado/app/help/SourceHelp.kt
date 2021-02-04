package io.legado.app.help

import android.os.Handler
import android.os.Looper
import io.legado.app.App
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.EncoderUtils
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.toastOnUi

object SourceHelp {

    private val handler = Handler(Looper.getMainLooper())
    private val list18Plus by lazy {
        try {
            return@lazy String(App.INSTANCE.assets.open("18PlusList.txt").readBytes())
                .splitNotBlank("\n")
        } catch (e: Exception) {
            return@lazy arrayOf<String>()
        }
    }

    fun insertRssSource(vararg rssSources: RssSource) {
        rssSources.forEach { rssSource ->
            if (is18Plus(rssSource.sourceUrl)) {
                handler.post {
                    App.INSTANCE.toastOnUi("${rssSource.sourceName}是18+网址,禁止导入.")
                }
            } else {
                App.db.rssSourceDao.insert(rssSource)
            }
        }
    }

    fun insertBookSource(vararg bookSources: BookSource) {
        bookSources.forEach { bookSource ->
            if (is18Plus(bookSource.bookSourceUrl)) {
                handler.post {
                    App.INSTANCE.toastOnUi("${bookSource.bookSourceName}是18+网址,禁止导入.")
                }
            } else {
                App.db.bookSourceDao.insert(bookSource)
            }
        }
    }

    private fun is18Plus(url: String?): Boolean {
        url ?: return false
        val baseUrl = NetworkUtils.getBaseUrl(url)
        baseUrl ?: return false
        if (AppConfig.isGooglePlay) return false
        try {
            val host = baseUrl.split("//", ".")
            val base64Url = EncoderUtils.base64Encode("${host[host.lastIndex - 1]}.${host.last()}")
            list18Plus.forEach {
                if (base64Url == it) {
                    return true
                }
            }
        } catch (e: Exception) {
        }
        return false
    }

}