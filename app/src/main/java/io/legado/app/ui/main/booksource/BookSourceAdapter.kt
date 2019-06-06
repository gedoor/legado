package io.legado.app.ui.main.booksource

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.BookSource

class BookSourceAdapter(context : Context) : PagedListAdapter<BookSource, BookSourceAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookSource>() {
            override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.id == newItem.id
                        && oldItem.name == newItem.name
                        && oldItem.group == newItem.group
                        && oldItem.isEnabled == newItem.isEnabled
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }
}