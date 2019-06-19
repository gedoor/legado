package io.legado.app.ui.main.findbook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import kotlinx.android.synthetic.main.item_find_book.view.*

class FindBookAdapter:PagedListAdapter<BookSource, FindBookAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        var DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookSource>() {
            override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.origin == newItem.origin

            override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.origin == newItem.origin
                        && oldItem.name == newItem.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_find_book, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        currentList?.get(position)?.let {
            holder.bind(it)
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(bookSource: BookSource) = with(itemView) {
            tv_name.text = bookSource.name
        }
    }
}