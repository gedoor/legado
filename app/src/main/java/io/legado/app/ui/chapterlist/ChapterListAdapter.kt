package io.legado.app.ui.chapterlist

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import kotlinx.android.synthetic.main.item_bookmark.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListAdapter(context: Context, val callback: Callback) :
    SimpleRecyclerAdapter<BookChapter>(context, R.layout.item_chapter_list) {

    override fun convert(holder: ItemViewHolder, item: BookChapter, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (callback.durChapterIndex() == item.index) {
                tv_chapter_name.setTextColor(context.accentColor)
            } else {
                tv_chapter_name.setTextColor(context.getCompatColor(R.color.tv_text_default))
            }
            tv_chapter_name.text = item.title
            this.onClick {
                callback.openChapter(item)
            }
            callback.book()?.let {
                tv_chapter_name.paint.isFakeBoldText = BookHelp.hasContent(it, item)
            }
        }
    }

    interface Callback {
        fun book(): Book?
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
    }
}