package io.legado.app.ui.booksource

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
        return oldItem.bookSourceName == newItem.bookSourceName
                && oldItem.bookSourceGroup == newItem.bookSourceGroup
                && oldItem.enabled == newItem.enabled
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return when {
            oldItem.bookSourceName == newItem.bookSourceName
                    && oldItem.bookSourceGroup == newItem.bookSourceGroup
                    && oldItem.enabled != newItem.enabled -> 2
            else -> null
        }
    }
}