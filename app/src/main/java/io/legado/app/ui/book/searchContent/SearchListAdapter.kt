package io.legado.app.ui.book.searchContent

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.text.HtmlCompat
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_search_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class SearchListAdapter(context: Context, val callback: Callback) :
    SimpleRecyclerAdapter<SearchResult>(context, R.layout.item_search_list) {

    val cacheFileNames = hashSetOf<String>()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun convert(holder: ItemViewHolder, item: SearchResult, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                // set search result color here
                tv_search_result.text = HtmlCompat.fromHtml(item.presentText, HtmlCompat.FROM_HTML_MODE_LEGACY)
            } else {
                //to do

            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callback.openSearchResult(it)
            }
        }
    }

    private fun upHasCache(itemView: View, isDur: Boolean, cached: Boolean) = itemView.apply {
        tv_search_result.paint.isFakeBoldText = cached
    }

    interface Callback {
        fun openSearchResult(searchResult: SearchResult)
    }
}