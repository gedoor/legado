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
import io.legado.app.databinding.ItemBookshelfGridGroupBinding
import io.legado.app.help.AppConfig
import io.legado.app.utils.invisible
import splitties.views.onLongClick

class BooksAdapterGrid(context: Context, callBack: CallBack) :
    BaseBooksAdapter<RecyclerView.ViewHolder>(context, callBack) {

    override fun getItemCount(): Int {
        return callBack.getItemCount()
    }

    override fun getItemViewType(position: Int): Int {
        return callBack.getItemType(position)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> GroupViewHolder(
                ItemBookshelfGridGroupBinding.inflate(LayoutInflater.from(context), parent, false)
            )
            else -> BookViewHolder(
                ItemBookshelfGridBinding.inflate(LayoutInflater.from(context), parent, false)
            )
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val bundle = payloads.getOrNull(0) as? Bundle
        when {
            bundle == null -> super.onBindViewHolder(holder, position, payloads)
            holder is BookViewHolder -> onBindBook(holder.binding, position, bundle)
            holder is GroupViewHolder -> onBindGroup(holder.binding, position, bundle)
        }
    }

    private fun onBindGroup(binding: ItemBookshelfGridGroupBinding, position: Int, bundle: Bundle) {
        binding.run {
            val item = callBack.getItem(position) as BookGroup
            tvName.text = item.groupName
            ivCover.load(item.cover)
        }
    }

    private fun onBindBook(binding: ItemBookshelfGridBinding, position: Int, bundle: Bundle) {
        binding.run {
            val item = callBack.getItem(position) as Book
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
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BookViewHolder -> onBindBook(holder.binding, position)
            is GroupViewHolder -> onBindGroup(holder.binding, position)
        }
    }

    private fun onBindGroup(binding: ItemBookshelfGridGroupBinding, position: Int) {
        binding.run {
            val item = callBack.getItem(position)
            if (item is BookGroup) {
                tvName.text = item.groupName
            }
            root.setOnClickListener {
                callBack.onItemClick(position)
            }
            root.onLongClick {
                callBack.onItemLongClick(position)
            }
        }
    }

    private fun onBindBook(binding: ItemBookshelfGridBinding, position: Int) {
        binding.run {
            val item = callBack.getItem(position)
            if (item is Book) {
                tvName.text = item.name
                ivCover.load(item.getDisplayCover(), item.name, item.author)
                upRefresh(this, item)
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

    class BookViewHolder(val binding: ItemBookshelfGridBinding) :
        RecyclerView.ViewHolder(binding.root)

    class GroupViewHolder(val binding: ItemBookshelfGridGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

}