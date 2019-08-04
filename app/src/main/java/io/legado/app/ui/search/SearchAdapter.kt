package io.legado.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.SearchBook

class SearchAdapter : PagedListAdapter<SearchBook, SearchAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchBook>() {
            override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean =
                oldItem.name == newItem.name
                        && oldItem.author == newItem.author

            override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean =
                oldItem.name == newItem.name
                        && oldItem.author == newItem.author
                        && oldItem.originCount == newItem.originCount
        }
    }

    var callBack: CallBack? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, callBack)
        }
    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(searchBook: SearchBook, callBack: CallBack?) {

        }
    }

    interface CallBack {
        fun showBookInfo()
    }
}