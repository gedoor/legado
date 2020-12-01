package io.legado.app.ui.book.changecover

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemCoverBinding
import org.jetbrains.anko.sdk27.listeners.onClick

class CoverAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_cover) {

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        ItemCoverBinding.bind(holder.itemView).apply {
            ivCover.load(item.coverUrl, item.name, item.author)
            tvSource.text = item.originName
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.changeTo(it.coverUrl ?: "")
                }
            }
        }
    }

    interface CallBack {
        fun changeTo(coverUrl: String)
    }
}