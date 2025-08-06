package io.legado.app.ui.rss.read

import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.RssSource
import io.legado.app.help.JsExtensions
import io.legado.app.ui.association.AddToBookshelfDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.utils.showDialogFragment
import org.json.JSONObject


@Suppress("unused")
class RssJsExtensions(private val activity: ReadRssActivity, private val rssSource: RssSource?) : JsExtensions {

    override fun getSource(): BaseSource? {
        return activity.getSource()
    }

    fun searchBook(key: String) {
        SearchActivity.start(activity, key)
    }

    fun addBook(bookUrl: String) {
        activity.showDialogFragment(AddToBookshelfDialog(bookUrl))
    }

    fun open(name: String, url: String) {
        return open(name,url,null)
    }
    fun open(name: String, url: String, title: String?) {
        val source = rssSource ?: return
        val sourceUrl = source.sourceUrl
        when (name) {
            "sort" -> {
                val sortSourceUrl = if (title != null) {
                    JSONObject().apply {
                        put(title, sourceUrl)
                    }.toString()
                }
                else {
                    sourceUrl
                }
                RssSortActivity.start(activity, url, sortSourceUrl)
            }
            "rss" -> {
                ReadRssActivity.start(activity, title ?: source.sourceName, url, sourceUrl)
            }
        }
    }

}
