package io.legado.app.ui.changesource

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import kotlinx.android.synthetic.main.item_change_source.view.*


class ChangeSourceAdapter(context: Context) : SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_change_source) {


    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        holder.itemView.apply {
            tv_origin.text = item.originName
            tv_last.text = item.latestChapterTitle
        }
    }


}