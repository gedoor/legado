package io.legado.app.ui.book.source.manage

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.BookSource

class DiffCallBack(
    private val oldItems: List<BookSource>,
    private val newItems: List<BookSource>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.bookSourceUrl == newItem.bookSourceUrl
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
        if (oldItem.bookSourceName != newItem.bookSourceName)
            return false
        if (oldItem.bookSourceGroup != newItem.bookSourceGroup)
            return false
        if (oldItem.enabled != newItem.enabled)
            return false
        return true
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
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
        if (oldItem.showExplore() != newItem.showExplore()) {
            payload.putBoolean("showExplore", newItem.enabledExplore)
        }
        if (payload.isEmpty) {
            return null
        }
        return payload
    }
}