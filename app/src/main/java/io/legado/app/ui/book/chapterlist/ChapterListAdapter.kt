package io.legado.app.ui.book.chapterlist

import android.content.Context
import android.widget.TextView
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
            if (payloads.isEmpty()) {
                if (callback.durChapterIndex() == item.index) {
                    tv_chapter_name.setTextColor(context.accentColor)
                } else {
                    tv_chapter_name.setTextColor(context.getCompatColor(R.color.primaryText))
                }
                tv_chapter_name.text = item.title
                if (!item.tag.isNullOrEmpty()) {
                    tv_tag.text = item.tag
                    tv_tag.visible()
                }
                upHasCache(
                    tv_chapter_name,
                    cacheFileNames.contains(BookHelp.formatChapterName(item))
                )
            } else {
                upHasCache(
                    tv_chapter_name,
                    cacheFileNames.contains(BookHelp.formatChapterName(item))
                )
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

    private fun upHasCache(textView: TextView, contains: Boolean) {
        textView.paint.isFakeBoldText = contains
    }

    interface Callback {
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
    }
}