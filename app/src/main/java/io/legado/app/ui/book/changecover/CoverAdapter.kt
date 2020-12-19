package io.legado.app.ui.book.changecover

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.base.adapter.DiffRecyclerAdapter
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemCoverBinding
import org.jetbrains.anko.sdk27.listeners.onClick

class CoverAdapter(context: Context, val callBack: CallBack) :
    DiffRecyclerAdapter<SearchBook, ItemCoverBinding>(context) {

    override val diffItemCallback: DiffUtil.ItemCallback<SearchBook>
        get() = object : DiffUtil.ItemCallback<SearchBook>() {
            override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

            override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem.originName == newItem.originName
                        && oldItem.coverUrl == newItem.coverUrl
            }

        }

    override fun getViewBinding(parent: ViewGroup): ItemCoverBinding {
        return ItemCoverBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemCoverBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) = with(binding) {
        ivCover.load(item.coverUrl, item.name, item.author)
        tvSource.text = item.originName
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