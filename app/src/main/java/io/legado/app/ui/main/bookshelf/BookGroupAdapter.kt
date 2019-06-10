package io.legado.app.ui.main.bookshelf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookGroup

class BookGroupAdapter : PagedListAdapter<BookGroup, BookGroupAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookGroup>() {
            override fun areItemsTheSame(oldItem: BookGroup, newItem: BookGroup): Boolean =
                    oldItem.groupId == newItem.groupId

            override fun areContentsTheSame(oldItem: BookGroup, newItem: BookGroup): Boolean =
                    oldItem.groupId == newItem.groupId
                            && oldItem.groupName == newItem.groupName
                            && oldItem.order == newItem.order
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_book_group, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }
}