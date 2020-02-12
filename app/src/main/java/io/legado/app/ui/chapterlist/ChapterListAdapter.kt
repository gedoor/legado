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
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_bookmark.view.tv_chapter_name
import kotlinx.android.synthetic.main.item_chapter_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListAdapter(context: Context, val callback: Callback) :
    SimpleRecyclerAdapter<BookChapter>(context, R.layout.item_chapter_list) {

    val cacheFileNames = arrayListOf<String>()

    override fun convert(holder: ItemViewHolder, item: BookChapter, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                if (callback.durChapterIndex() == item.index) {
                    tv_chapter_name.setTextColor(context.accentColor)
                } else {
                    tv_chapter_name.setTextColor(context.getCompatColor(R.color.tv_text_default))
                }
                tv_chapter_name.text = item.title
                if (!item.tag.isNullOrEmpty()) {
                    tv_tag.text = item.tag
                    tv_tag.visible()
                }
                this.onClick {
                    callback.openChapter(item)
                }
                tv_chapter_name.paint.isFakeBoldText =
                    cacheFileNames.contains(BookHelp.formatChapterName(item))
            } else {
                tv_chapter_name.paint.isFakeBoldText =
                    cacheFileNames.contains(BookHelp.formatChapterName(item))
            }
        }
    }

    interface Callback {
        fun book(): Book?
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
    }
}