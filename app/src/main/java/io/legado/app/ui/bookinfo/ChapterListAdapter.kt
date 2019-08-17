package io.legado.app.ui.bookinfo

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookChapter
import kotlinx.android.synthetic.main.item_chapter_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListAdapter(context: Context) : SimpleRecyclerAdapter<BookChapter>(context, R.layout.item_chapter_list) {
    var callBack: CallBack? = null

    override fun convert(holder: ItemViewHolder, item: BookChapter, payloads: MutableList<Any>) {
        holder.itemView.apply {
            tv_chapter_name.text = item.title
            this.onClick {
                callBack?.skipToChapter(item.index)
            }
        }
    }

    interface CallBack {
        fun skipToChapter(index: Int)
    }
}