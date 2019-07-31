package io.legado.app.ui.sourcedebug

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import kotlinx.android.synthetic.main.item_source_debug.view.*

class SourceDebugAdapter(context: Context) : SimpleRecyclerAdapter<String>(context, R.layout.item_source_debug) {
    override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
        holder.itemView.apply {
            text_view.text = item
        }
    }
}