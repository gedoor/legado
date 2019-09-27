package io.legado.app.ui.main.bookshelf

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookGroup
import kotlinx.android.synthetic.main.item_book_group.view.*

class BookGroupAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<BookGroup>(context, R.layout.item_book_group) {

    override fun convert(holder: ItemViewHolder, item: BookGroup, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_group.text = item.groupName
            tv_group.setOnClickListener { callBack.open(item) }
        }
    }

    interface CallBack {
        fun open(bookGroup: BookGroup)
    }
}