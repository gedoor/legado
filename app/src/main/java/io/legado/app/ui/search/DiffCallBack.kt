package io.legado.app.ui.search

import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.SearchShow

class DiffCallBack(private val oldItems: List<SearchShow>, private val newItems: List<SearchShow>) :
    DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].name == newItems[newItemPosition].name
                && oldItems[oldItemPosition].author == newItems[newItemPosition].author
    }

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].originCount == newItems[newItemPosition].originCount
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}