package io.legado.app.ui.rss.source.debug

import android.content.Context
import android.view.View
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import kotlinx.android.synthetic.main.item_log.view.*

class RssSourceDebugAdapter(context: Context) :
    SimpleRecyclerAdapter<String>(context, R.layout.item_log) {
    override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
        holder.itemView.apply {
            if (text_view.getTag(R.id.tag1) == null) {
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        text_view.isCursorVisible = false
                        text_view.isCursorVisible = true
                    }

                    override fun onViewDetachedFromWindow(v: View) {}
                }
                text_view.addOnAttachStateChangeListener(listener)
                text_view.setTag(R.id.tag1, listener)
            }
            text_view.text = item
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        //nothing
    }
}