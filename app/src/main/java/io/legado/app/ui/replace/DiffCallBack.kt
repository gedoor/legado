package io.legado.app.ui.replace

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.ReplaceRule

class DiffCallBack : DiffUtil.ItemCallback<ReplaceRule>() {

    override fun areItemsTheSame(oldItem: ReplaceRule, newItem: ReplaceRule): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ReplaceRule, newItem: ReplaceRule): Boolean {
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

    override fun getChangePayload(oldItem: ReplaceRule, newItem: ReplaceRule): Any? {
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