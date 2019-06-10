package io.legado.app.ui.main.bookshelf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookGroup
import kotlinx.android.synthetic.main.item_book_group.view.*

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

    var callBack: CallBack? = null
    private val defaultGroups = arrayOf(BookGroup(-5, "全部"),
            BookGroup(-5, "本地"),
            BookGroup(-5, "音频"))

    override fun getItemCount(): Int {
        return super.getItemCount() + defaultGroups.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_book_group, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (position < defaultGroups.size) {
            holder.bind(defaultGroups[position], callBack)
        } else {
            currentList?.get(position - defaultGroups.size)?.let {
                holder.bind(it, callBack)
            }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(bookGroup: BookGroup, callBack: CallBack?) = with(itemView) {
            tv_group.text = bookGroup.groupName
        }
    }

    interface CallBack {

    }
}