package io.legado.app.ui.main.bookshelf.style2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.ItemBookshelfGridBinding
import io.legado.app.help.AppConfig
import io.legado.app.utils.invisible
import splitties.views.onLongClick

class BooksAdapterGrid(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter<BooksAdapterGrid.ItemViewHolder>(context) {

    override fun getItemCount(): Int {
        return callBack.getItemCount()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        return ItemViewHolder(
            ItemBookshelfGridBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.binding.run {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                when (val item = callBack.getItem(position)) {
                    is Book -> {
                        bundle.keySet().forEach {
                            when (it) {
                                "name" -> tvName.text = item.name
                                "cover" -> ivCover.load(
                                    item.getDisplayCover(),
                                    item.name,
                                    item.author
                                )
                                "refresh" -> upRefresh(this, item)
                            }
                        }
                    }
                    is BookGroup -> {
                        tvName.text = item.groupName
                    }
                }

            }
        }
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.run {
            when (val item = callBack.getItem(position)) {
                is Book -> {
                    tvName.text = item.name
                    ivCover.load(item.getDisplayCover(), item.name, item.author)
                    upRefresh(this, item)
                }
                is BookGroup -> {
                    tvName.text = item.groupName
                }
            }
            root.setOnClickListener {
                callBack.onItemClick(position)
            }
            root.onLongClick {
                callBack.onItemLongClick(position)
            }
        }
    }

    private fun upRefresh(binding: ItemBookshelfGridBinding, item: Book) {
        if (item.origin != BookType.local && callBack.isUpdate(item.bookUrl)) {
            binding.bvUnread.invisible()
            binding.rlLoading.show()
        } else {
            binding.rlLoading.hide()
            if (AppConfig.showUnread) {
                binding.bvUnread.setBadgeCount(item.getUnreadChapterNum())
                binding.bvUnread.setHighlight(item.lastCheckCount > 0)
            } else {
                binding.bvUnread.invisible()
            }
        }
    }

    class ItemViewHolder(val binding: ItemBookshelfGridBinding) :
        RecyclerView.ViewHolder(binding.root)

}