package io.legado.app.ui.book.toc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.Bookmark
import kotlinx.android.synthetic.main.item_bookmark.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick


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
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookmark, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, callback)
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(bookmark: Bookmark, callback: Callback?) = with(itemView) {
            tv_chapter_name.text = bookmark.chapterName
            tv_content.text = bookmark.content
            itemView.onClick {
                callback?.onClick(bookmark)
            }
            itemView.onLongClick {
                callback?.onLongClick(bookmark)
                true
            }
        }
    }

    interface Callback {
        fun onClick(bookmark: Bookmark)
        fun onLongClick(bookmark: Bookmark)
    }
}