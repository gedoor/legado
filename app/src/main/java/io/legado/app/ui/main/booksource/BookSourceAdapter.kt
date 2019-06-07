package io.legado.app.ui.main.booksource

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import kotlinx.android.synthetic.main.item_book_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class BookSourceAdapter : PagedListAdapter<BookSource, BookSourceAdapter.MyViewHolder>(DIFF_CALLBACK) {

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

    var callback: Callback? = null
    val checkedList = HashSet<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_book_source, parent, false))
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, checkedList, callback) }
    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(bookSource: BookSource, checkedList: HashSet<String>, callback: Callback?) = with(itemView) {
            cb_book_source.text = String.format("%s (%s)", bookSource.name, bookSource.group)
            cb_book_source.onClick {
                if (cb_book_source.isChecked) {
                    checkedList.add(bookSource.origin)
                } else {
                    checkedList.remove(bookSource.origin)
                }
            }
        }
    }

    interface Callback {
        fun del(bookSource: BookSource)
        fun edit(bookSource: BookSource)
        fun update(bookSource: BookSource)
    }
}