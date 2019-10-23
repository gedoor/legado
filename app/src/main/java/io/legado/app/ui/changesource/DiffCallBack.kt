package io.legado.app.ui.changesource

import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.SearchBook

class DiffCallBack(private val oldItems: List<SearchBook>, private val newItems: List<SearchBook>) :
    DiffUtil.Callback() {


    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].bookUrl == newItems[newItemPosition].bookUrl
    }

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].originName == newItems[newItemPosition].originName
                && oldItems[oldItemPosition].latestChapterTitle == newItems[newItemPosition].latestChapterTitle
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        if (oldItem.originName != newItem.originName || oldItem.latestChapterTitle != newItem.latestChapterTitle) {
            return true
        }
        return null
    }

}