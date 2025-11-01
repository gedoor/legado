package io.legado.app.ui.rss.read

import androidx.lifecycle.lifecycleScope
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.help.JsExtensions
import io.legado.app.ui.association.AddToBookshelfDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.utils.showDialogFragment
import kotlinx.coroutines.launch
import org.json.JSONObject


@Suppress("unused")
class RssJsExtensions(private val activity: ReadRssActivity) : JsExtensions {

    override fun getSource(): RssSource? {
        return activity.getSource()
    }

    fun put(key: String, value: String): String {
        getSource()?.put(key, value)
        return value
    }

    fun get(key: String): String {
        return getSource()?.get(key)  ?: ""
    }
    fun searchBook(key: String) {
        searchBook(key, null)
    }
    fun searchBook(key: String, searchScope: String?) {
        SearchActivity.start(activity, key, searchScope)
    }

    fun addBook(bookUrl: String) {
        activity.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

    fun open(name: String, url: String) {
        return open(name,url,null)
    }
    fun open(name: String, url: String, title: String?) {
        activity.lifecycleScope.launch{
            val source = getSource() ?: return@launch
            when (name) {
                "sort" -> {
                    val sortUrl = title?.let {
                        JSONObject().put(title, url).toString()
                    } ?: url
                    RssSortActivity.start(activity, sortUrl, source.sourceUrl)
                }
                "rss" -> {
                    val title = title ?: source.sourceName
                    val origin = source.sourceUrl
                    val rssReadRecord = RssReadRecord(
                        record = url,
                        title = title,
                        origin = origin,
                        readTime = System.currentTimeMillis())
                    appDb.rssReadRecordDao.insertRecord(rssReadRecord)
                    ReadRssActivity.start(activity, title, url, origin)
                }
            }
        }
    }

}
