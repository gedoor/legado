package io.legado.app.ui.main.bookshelf.style2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.ItemBookshelfListBinding
import io.legado.app.databinding.ItemBookshelfListGroupBinding
import io.legado.app.help.AppConfig
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import splitties.views.onLongClick

class BooksAdapterList(context: Context, callBack: CallBack) :
    BaseBooksAdapter<RecyclerView.ViewHolder>(context, callBack) {

    override fun getItemCount(): Int {
        return callBack.getItemCount()
    }

    override fun getItemViewType(position: Int): Int {
        return callBack.getItemType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> GroupViewHolder(
                ItemBookshelfListGroupBinding.inflate(LayoutInflater.from(context), parent, false)
            )
            else -> BookViewHolder(
                ItemBookshelfListBinding.inflate(LayoutInflater.from(context), parent, false)
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

    private fun onBindGroup(
        binding: ItemBookshelfListGroupBinding,
        position: Int,
        @Suppress("UNUSED_PARAMETER") bundle: Bundle
    ) {
        binding.run {
            val item = callBack.getItem(position) as BookGroup
            tvName.text = item.groupName
            ivCover.load(item.cover)
        }
    }

    private fun onBindBook(binding: ItemBookshelfListBinding, position: Int, bundle: Bundle) {
        binding.run {
            val item = callBack.getItem(position) as Book
            tvRead.text = item.durChapterTitle
            tvLast.text = item.latestChapterTitle
            bundle.keySet().forEach {
                when (it) {
                    "name" -> tvName.text = item.name
                    "author" -> tvAuthor.text = item.author
                    "cover" -> ivCover.load(item.getDisplayCover(), item.name, item.author)
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

    private fun onBindGroup(binding: ItemBookshelfListGroupBinding, position: Int) {
        binding.run {
            val item = callBack.getItem(position)
            if (item is BookGroup) {
                tvName.text = item.groupName
                ivCover.load(item.cover)
                flHasNew.gone()
                ivAuthor.gone()
                ivLast.gone()
                ivRead.gone()
                tvAuthor.gone()
                tvLast.gone()
                tvRead.gone()
            }
            root.setOnClickListener {
                callBack.onItemClick(position)
            }
            root.onLongClick {
                callBack.onItemLongClick(position)
            }
        }
    }

    private fun onBindBook(binding: ItemBookshelfListBinding, position: Int) {
        binding.run {
            val item = callBack.getItem(position)
            if (item is Book) {
                tvName.text = item.name
                tvAuthor.text = item.author
                tvRead.text = item.durChapterTitle
                tvLast.text = item.latestChapterTitle
                ivCover.load(item.getDisplayCover(), item.name, item.author)
                flHasNew.visible()
                ivAuthor.visible()
                ivLast.visible()
                ivRead.visible()
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

    class BookViewHolder(val binding: ItemBookshelfListBinding) :
        RecyclerView.ViewHolder(binding.root)

    class GroupViewHolder(val binding: ItemBookshelfListGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

}