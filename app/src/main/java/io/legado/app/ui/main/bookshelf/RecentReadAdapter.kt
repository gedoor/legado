package io.legado.app.ui.main.bookshelf

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.Book

class RecentReadAdapter : PagedListAdapter<Book, RecentReadAdapter.MyViewHolder>(DIFF_CALLBACK) {

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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }
}