package io.legado.app.ui.book.searchContent

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.hexString
import kotlinx.android.synthetic.main.item_search_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class SearchContentAdapter(context: Context, val callback: Callback) :
    SimpleRecyclerAdapter<SearchResult>(context, R.layout.item_search_list) {

    val cacheFileNames = hashSetOf<String>()
    val textColor = context.getCompatColor(R.color.primaryText).hexString.substring(2)
    val accentColor = context.accentColor.hexString.substring(2)

    override fun convert(holder: ItemViewHolder, item: SearchResult, payloads: MutableList<Any>) {
        with(holder.itemView) {
            val isDur = callback.durChapterIndex() == item.chapterIndex
            if (payloads.isEmpty()) {
                tv_search_result.text = item.getHtmlCompat(textColor, accentColor)
                tv_search_result.paint.isFakeBoldText = isDur
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

    interface Callback {
        fun openSearchResult(searchResult: SearchResult)
        fun durChapterIndex(): Int
    }
}