package io.legado.app.ui.main.bookshelf.books

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemBookshelfListBinding
import io.legado.app.help.AppConfig
import io.legado.app.utils.invisible
import splitties.views.onLongClick

class BooksAdapterList(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter<ItemBookshelfListBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookshelfListBinding {
        return ItemBookshelfListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookshelfListBinding,
        item: Book,
        payloads: MutableList<Any>
    ) = with(binding) {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            tvName.text = item.name
            tvAuthor.text = item.author
            tvRead.text = item.durChapterTitle
            tvLast.text = item.latestChapterTitle
            ivCover.load(item.getDisplayCover(), item.name, item.author)
            upRefresh(binding, item)
        } else {
            tvRead.text = item.durChapterTitle
            tvLast.text = item.latestChapterTitle
            bundle.keySet().forEach {
                when (it) {
                    "name" -> tvName.text = item.name
                    "author" -> tvAuthor.text = item.author
                    "cover" -> ivCover.load(item.getDisplayCover(), item.name, item.author)
                    "refresh" -> upRefresh(binding, item)
                }
            }
        }
    }

    private fun upRefresh(binding: ItemBookshelfListBinding, item: Book) {
        if (item.origin != BookType.local && callBack.isUpdate(item.bookUrl)) {
            binding.bvUnread.invisible()
            binding.rlLoading.show()
        } else {
            binding.rlLoading.hide()
            if (AppConfig.showUnread) {
                binding.bvUnread.setHighlight(item.lastCheckCount > 0)
                binding.bvUnread.setBadgeCount(item.getUnreadChapterNum())
            } else {
                binding.bvUnread.invisible()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookshelfListBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.open(it)
                }
            }

            onLongClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openBookInfo(it)
                }
            }
        }
    }
}