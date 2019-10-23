package io.legado.app.ui.replacerule

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
        return oldItem.name == newItem.name
                && oldItem.group == newItem.group
                && oldItem.isEnabled == newItem.isEnabled
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return when {
            oldItem.name == newItem.name
                    && oldItem.group == newItem.group
                    && oldItem.isEnabled != newItem.isEnabled -> 2
            else -> null
        }
    }
}