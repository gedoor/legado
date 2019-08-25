package io.legado.app.ui.main.rss

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.RssSource

class RssAdapter : PagedListAdapter<RssSource, RssAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RssSource>() {
            override fun areItemsTheSame(oldItem: RssSource, newItem: RssSource): Boolean =
                oldItem.sourceUrl == newItem.sourceUrl

            override fun areContentsTheSame(oldItem: RssSource, newItem: RssSource): Boolean =
                oldItem.sourceName == newItem.sourceName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_rss,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let {
            holder.onBind(it)
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun onBind(rssSource: RssSource) {

        }

    }
}