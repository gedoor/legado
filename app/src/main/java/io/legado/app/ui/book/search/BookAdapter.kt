package io.legado.app.ui.book.search

import android.content.Context
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemFilletTextBinding
import org.jetbrains.anko.sdk27.listeners.onClick

class BookAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<Book, ItemFilletTextBinding>(context) {

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFilletTextBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        with(binding) {
            textView.text = item.name
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFilletTextBinding) {
        holder.itemView.apply {
            onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.showBookInfo(it)
                }
            }
        }
    }

    interface CallBack {
        fun showBookInfo(book: Book)
    }
}