package io.legado.app.ui.rss.source.manage

import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.RssSource

class DiffCallBack(
    private val oldItems: List<RssSource>,
    private val newItems: List<RssSource>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.sourceUrl == newItem.sourceUrl
    }

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.sourceName == newItem.sourceName
                && oldItem.enabled == newItem.enabled
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return when {
            oldItem.sourceName == newItem.sourceName
                    && oldItem.enabled != newItem.enabled -> 2
            else -> null
        }
    }
}