package io.legado.app.ui.book.source.debug

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import kotlinx.android.synthetic.main.item_log.view.*

class SourceDebugAdapter(context: Context) :
    SimpleRecyclerAdapter<String>(context, R.layout.item_log) {
    override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
        holder.itemView.apply {
            text_view.text = item
        }
    }
}