package io.legado.app.ui.replace

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.ReplaceRule

class DiffCallBack(
    private val oldItems: List<ReplaceRule>,
    private val newItems: List<ReplaceRule>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.id == newItem.id
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
        if (oldItem.name != newItem.name) {
            return false
        }
        if (oldItem.group != newItem.group) {
            return false
        }
        if (oldItem.isEnabled != newItem.isEnabled) {
            return false
        }
        return true
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        val payload = Bundle()
        if (oldItem.name != newItem.name) {
            payload.putString("name", newItem.name)
        }
        if (oldItem.group != newItem.group) {
            payload.putString("group", newItem.group)
        }
        if (oldItem.isEnabled != newItem.isEnabled) {
            payload.putBoolean("enabled", newItem.isEnabled)
        }
        if (payload.isEmpty) {
            return null
        }
        return payload
    }
}