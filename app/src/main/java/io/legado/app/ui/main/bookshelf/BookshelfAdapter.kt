package io.legado.app.ui.main.bookshelf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.Book

class BookshelfAdapter : PagedListAdapter<Book, BookshelfAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
                    oldItem.descUrl == newItem.descUrl

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
                    oldItem.descUrl == newItem.descUrl
                            && oldItem.durChapterTitle == newItem.durChapterTitle
                            && oldItem.latestChapterTitle == newItem.latestChapterTitle
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookshelf_list, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }
}