package io.legado.app.ui.chapterlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookChapter
import kotlinx.android.synthetic.main.item_bookmark.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListAdapter : PagedListAdapter<BookChapter, ChapterListAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookChapter>() {
            override fun areItemsTheSame(oldItem: BookChapter, newItem: BookChapter): Boolean =
                oldItem.bookUrl == newItem.bookUrl
                        && oldItem.url == newItem.url

            override fun areContentsTheSame(oldItem: BookChapter, newItem: BookChapter): Boolean =
                oldItem.title == newItem.title
        }
    }

    var callback: Callback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chapter_list, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, callback)
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(bookChapter: BookChapter, callback: Callback?) = with(itemView) {
            tv_chapter_name.text = bookChapter.title

            itemView.onClick {
                callback?.openChapter()
            }
        }
    }

    interface Callback {
        fun openChapter()
    }
}