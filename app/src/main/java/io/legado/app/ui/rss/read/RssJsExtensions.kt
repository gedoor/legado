package io.legado.app.ui.rss.read

import androidx.lifecycle.lifecycleScope
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.RssSource
import io.legado.app.help.JsExtensions
import io.legado.app.ui.association.AddToBookshelfDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.utils.showDialogFragment
import kotlinx.coroutines.launch
import org.json.JSONObject


@Suppress("unused")
class RssJsExtensions(private val activity: ReadRssActivity, private val rssSource: RssSource?) : JsExtensions {

    override fun getSource(): BaseSource? {
        return activity.getSource()
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
            val source = rssSource ?: return@launch
            when (name) {
                "sort" -> {
                    val sortSourceUrl = title?.let {
                        JSONObject().put(title, source.sourceUrl).toString()
                    } ?: source.sourceUrl
                    RssSortActivity.start(activity, url, sortSourceUrl)
                }
                "rss" -> {
                    ReadRssActivity.start(activity, title ?: source.sourceName, url, source.sourceUrl)
                }
            }
        }
    }

}
