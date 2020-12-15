package io.legado.app.ui.book.source.manage

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.BookSource

class DiffCallBack : DiffUtil.ItemCallback<BookSource>() {

    override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
        return oldItem.bookSourceUrl == newItem.bookSourceUrl
    }

    override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
        if (oldItem.bookSourceName != newItem.bookSourceName)
            return false
        if (oldItem.bookSourceGroup != newItem.bookSourceGroup)
            return false
        if (oldItem.enabled != newItem.enabled)
            return false
        if (oldItem.enabledExplore != newItem.enabledExplore
            || oldItem.exploreUrl != newItem.exploreUrl
        ) {
            return false
        }
        return true
    }

    override fun getChangePayload(oldItem: BookSource, newItem: BookSource): Any? {
        val payload = Bundle()
        if (oldItem.bookSourceName != newItem.bookSourceName) {
            payload.putString("name", newItem.bookSourceName)
        }
        if (oldItem.bookSourceGroup != newItem.bookSourceGroup) {
            payload.putString("group", newItem.bookSourceGroup)
        }
        if (oldItem.enabled != newItem.enabled) {
            payload.putBoolean("enabled", newItem.enabled)
        }
        if (oldItem.enabledExplore != newItem.enabledExplore
            || oldItem.exploreUrl != newItem.exploreUrl
        ) {
            payload.putBoolean("showExplore", true)
        }
        if (payload.isEmpty) {
            return null
        }
        return payload
    }
}