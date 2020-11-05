package io.legado.app.ui.book.toc

import android.content.Context
import android.view.View
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_bookmark.view.tv_chapter_name
import kotlinx.android.synthetic.main.item_chapter_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListAdapter(context: Context, val callback: Callback) :
    SimpleRecyclerAdapter<BookChapter>(context, R.layout.item_chapter_list) {

    val cacheFileNames = hashSetOf<String>()

    override fun convert(holder: ItemViewHolder, item: BookChapter, payloads: MutableList<Any>) {
        with(holder.itemView) {
            val isDur = callback.durChapterIndex() == item.index
            val cached = callback.isLocalBook
                    || cacheFileNames.contains(BookHelp.formatChapterName(item))
            if (payloads.isEmpty()) {
                if (isDur) {
                    tv_chapter_name.setTextColor(context.accentColor)
                } else {
                    tv_chapter_name.setTextColor(context.getCompatColor(R.color.primaryText))
                }
                tv_chapter_name.text = item.title
                if (!item.tag.isNullOrEmpty()) {
                    tv_tag.text = item.tag
                    tv_tag.visible()
                }
                upHasCache(this, isDur, cached)
            } else {
                upHasCache(this, isDur, cached)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callback.openChapter(it)
            }
        }
    }

    private fun upHasCache(itemView: View, isDur: Boolean, cached: Boolean) = itemView.apply {
        tv_chapter_name.paint.isFakeBoldText = cached
        iv_checked.setImageResource(R.drawable.ic_outline_cloud_24)
        iv_checked.visible(!cached)
        if (isDur) {
            iv_checked.setImageResource(R.drawable.ic_check)
            iv_checked.visible()
        }
    }

    interface Callback {
        val isLocalBook: Boolean
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
    }
}