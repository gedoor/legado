package io.legado.app.ui.main.bookshelf.style2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.ItemBookshelfListBinding
import io.legado.app.databinding.ItemBookshelfListGroupBinding
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import splitties.views.onLongClick

@Suppress("UNUSED_PARAMETER")
class BooksAdapterList(context: Context, callBack: CallBack) :
    BaseBooksAdapter<RecyclerView.ViewHolder>(context, callBack) {

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
            holder is BookViewHolder -> (callBack.getItem(position) as? Book)?.let {
                holder.onBind(it, bundle)
            }
            holder is GroupViewHolder -> (callBack.getItem(position) as? BookGroup)?.let {
                holder.onBind(it, bundle)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BookViewHolder -> (callBack.getItem(position) as? Book)?.let {
                holder.onBind(it, position)
            }
            is GroupViewHolder -> (callBack.getItem(position) as? BookGroup)?.let {
                holder.onBind(it, position)
            }
        }
    }

    inner class BookViewHolder(val binding: ItemBookshelfListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(item: Book, position: Int) = binding.run{
            tvName.text = item.name
            tvAuthor.text = item.author
            tvRead.text = item.durChapterTitle
            tvLast.text = item.latestChapterTitle
            ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
            flHasNew.visible()
            ivAuthor.visible()
            ivLast.visible()
            ivRead.visible()
            upRefresh(this, item)
            root.setOnClickListener {
                callBack.onItemClick(position)
            }
            root.onLongClick {
                callBack.onItemLongClick(position)
            }
        }

        fun onBind(item: Book, bundle: Bundle) = binding.run {
            tvRead.text = item.durChapterTitle
            tvLast.text = item.latestChapterTitle
            bundle.keySet().forEach {
                when (it) {
                    "name" -> tvName.text = item.name
                    "author" -> tvAuthor.text = item.author
                    "cover" -> ivCover.load(item.getDisplayCover(), item.name, item.author, false, item.origin)
                    "refresh" -> upRefresh(this, item)
                }
            }
        }

        private fun upRefresh(binding: ItemBookshelfListBinding, item: Book) {
            if (!item.isLocal && callBack.isUpdate(item.bookUrl)) {
                binding.bvUnread.invisible()
                binding.rlLoading.visible()
            } else {
                binding.rlLoading.gone()
                if (AppConfig.showUnread) {
                    binding.bvUnread.setHighlight(item.lastCheckCount > 0)
                    binding.bvUnread.setBadgeCount(item.getUnreadChapterNum())
                } else {
                    binding.bvUnread.invisible()
                }
            }
        }

    }

    inner class GroupViewHolder(val binding: ItemBookshelfListGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(item: BookGroup, position: Int) = binding.run{
            tvName.text = item.groupName
            ivCover.load(item.cover)
            flHasNew.gone()
            ivAuthor.gone()
            ivLast.gone()
            ivRead.gone()
            tvAuthor.gone()
            tvLast.gone()
            tvRead.gone()
            root.setOnClickListener {
                callBack.onItemClick(position)
            }
            root.onLongClick {
                callBack.onItemLongClick(position)
            }
        }

        fun onBind(item: BookGroup, bundle: Bundle) = binding.run {
            tvName.text = item.groupName
            ivCover.load(item.cover)
        }

    }

}