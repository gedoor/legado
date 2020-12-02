package io.legado.app.ui.book.changecover

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemCoverBinding
import org.jetbrains.anko.sdk27.listeners.onClick

class CoverAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchBook, ItemCoverBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemCoverBinding {
        return ItemCoverBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemCoverBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) {
        binding.apply {
            ivCover.load(item.coverUrl, item.name, item.author)
            tvSource.text = item.originName
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemCoverBinding) {
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