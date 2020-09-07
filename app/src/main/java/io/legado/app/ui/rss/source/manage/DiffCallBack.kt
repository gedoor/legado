package io.legado.app.ui.rss.source.manage

import android.os.Bundle
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
                && oldItem.sourceGroup == newItem.sourceGroup
                && oldItem.enabled == newItem.enabled
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        val payload = Bundle()
        if (oldItem.sourceName != newItem.sourceName) {
            payload.putString("name", newItem.sourceName)
        }
        if (oldItem.sourceGroup != newItem.sourceGroup) {
            payload.putString("group", newItem.sourceGroup)
        }
        if (oldItem.enabled != newItem.enabled) {
            payload.putBoolean("enabled", newItem.enabled)
        }
        if (payload.isEmpty) {
            return null
        }
        return payload
    }
}