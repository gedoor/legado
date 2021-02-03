package io.legado.app.ui.book.toc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.ItemBookmarkBinding
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick

class BookmarkAdapter(val callback: Callback) : PagedListAdapter<Bookmark, BookmarkAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Bookmark>() {
            override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
                oldItem.time == newItem.time

            override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean =
                oldItem.time == newItem.time
                        && oldItem.bookUrl == newItem.bookUrl
                        && oldItem.chapterName == newItem.chapterName
                        && oldItem.content == newItem.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, callback)
        }
    }

    class MyViewHolder(val binding: ItemBookmarkBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: Bookmark, callback: Callback?) = with(binding) {
            tvChapterName.text = bookmark.chapterName
            tvBookText.text = bookmark.bookText
            tvContent.text = bookmark.content
            itemView.onClick {
                callback?.onClick(bookmark)
            }
            itemView.onLongClick(returnValue = true) {
                callback?.onLongClick(bookmark)
            }
        }
    }

    interface Callback {
        fun onClick(bookmark: Bookmark)
        fun onLongClick(bookmark: Bookmark)
    }
}