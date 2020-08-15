package io.legado.app.ui.book.info

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.lib.theme.accentColor
import kotlinx.android.synthetic.main.item_chapter_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.textColorResource

class ChapterListAdapter(context: Context, var callBack: CallBack) :
    SimpleRecyclerAdapter<BookChapter>(context, R.layout.item_chapter_list) {

    override fun convert(holder: ItemViewHolder, item: BookChapter, payloads: MutableList<Any>) {
        holder.itemView.apply {
            tv_chapter_name.text = item.title
            if (item.index == callBack.durChapterIndex()) {
                tv_chapter_name.setTextColor(context.accentColor)
            } else {
                tv_chapter_name.textColorResource = R.color.secondaryText
            }

        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            this.onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openChapter(it)
                }
            }
        }
    }

    interface CallBack {
        fun openChapter(chapter: BookChapter)
        fun durChapterIndex(): Int
    }
}