package io.legado.app.ui.rss.source.manage

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.RssSource

class DiffCallBack : DiffUtil.ItemCallback<RssSource>() {

    override fun areItemsTheSame(oldItem: RssSource, newItem: RssSource): Boolean {
        return oldItem.sourceUrl == newItem.sourceUrl
    }

    override fun areContentsTheSame(oldItem: RssSource, newItem: RssSource): Boolean {
        return oldItem.sourceName == newItem.sourceName
                && oldItem.sourceGroup == newItem.sourceGroup
                && oldItem.enabled == newItem.enabled
    }

    override fun getChangePayload(oldItem: RssSource, newItem: RssSource): Any? {
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