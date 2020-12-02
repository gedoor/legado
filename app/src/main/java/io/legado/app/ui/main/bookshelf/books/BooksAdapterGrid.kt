package io.legado.app.ui.main.bookshelf.books

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemBookshelfGridBinding
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.invisible
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class BooksAdapterGrid(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter<ItemBookshelfGridBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookshelfGridBinding {
        return ItemBookshelfGridBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookshelfGridBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            ATH.applyBackgroundTint(binding.root)
            binding.tvName.text = item.name
            binding.ivCover.load(item.getDisplayCover(), item.name, item.author)
            upRefresh(binding, item)
        } else {
            bundle.keySet().forEach {
                when (it) {
                    "name" -> binding.tvName.text = item.name
                    "cover" -> binding.ivCover.load(item.getDisplayCover(), item.name, item.author)
                    "refresh" -> upRefresh(binding, item)
                }
            }
        }
    }

    private fun upRefresh(binding: ItemBookshelfGridBinding, item: Book) {
        if (item.origin != BookType.local && callBack.isUpdate(item.bookUrl)) {
            binding.bvUnread.invisible()
            binding.rlLoading.show()
        } else {
            binding.rlLoading.hide()
            binding.bvUnread.setBadgeCount(item.getUnreadChapterNum())
            binding.bvUnread.setHighlight(item.lastCheckCount > 0)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookshelfGridBinding) {
        holder.itemView.apply {
            onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.open(it)
                }
            }

            onLongClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openBookInfo(it)
                }
                true
            }
        }
    }
}