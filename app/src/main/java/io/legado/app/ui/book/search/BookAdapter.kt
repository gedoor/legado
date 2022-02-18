package io.legado.app.ui.book.search

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemFilletTextBinding


class BookAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<Book, ItemFilletTextBinding>(context) {

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getViewBinding(parent: ViewGroup): ItemFilletTextBinding {
        return ItemFilletTextBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFilletTextBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        binding.run {
            textView.text = item.name
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFilletTextBinding) {
        holder.itemView.apply {
            setOnClickListener {
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