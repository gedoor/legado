package io.legado.app.ui.book.searchContent

import android.content.Context
import android.os.Build
import android.text.Html
import android.util.Log
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
import kotlinx.android.synthetic.main.item_bookmark.view.*
import kotlinx.android.synthetic.main.item_search_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class SearchListAdapter(context: Context, val callback: Callback) :
    SimpleRecyclerAdapter<SearchResult>(context, R.layout.item_search_list) {

    val cacheFileNames = hashSetOf<String>()

    override fun convert(holder: ItemViewHolder, item: SearchResult, payloads: MutableList<Any>) {
        with(holder.itemView) {
            val isDur = callback.durChapterIndex() == item.chapterIndex
            if (payloads.isEmpty()) {
                tv_search_result.text = item.parseText(item.presentText)
                if (isDur){
                    tv_search_result.paint.isFakeBoldText = true
                }
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