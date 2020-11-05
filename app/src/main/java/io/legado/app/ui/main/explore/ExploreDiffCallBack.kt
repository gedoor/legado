package io.legado.app.ui.main.explore

import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.BookSource


class ExploreDiffCallBack(
    private val oldItems: List<BookSource>,
    private val newItems: List<BookSource>
) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return true
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        if (oldItem.bookSourceName != newItem.bookSourceName) {
            return false
        }
        return true
    }

}