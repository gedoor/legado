package io.legado.app.ui.rss.read

import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.RssSource
import io.legado.app.help.JsExtensions
import io.legado.app.ui.association.AddToBookshelfDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.utils.showDialogFragment



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

    fun open(name: String,url: String) {
        when (name) {
            "sort" -> RssSortActivity.start(activity,url,rssSource?.sourceUrl)
        }
    }

}
