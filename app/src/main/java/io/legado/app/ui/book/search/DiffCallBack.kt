package io.legado.app.ui.book.search

import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.SearchBook

class DiffCallBack(private val oldItems: List<SearchBook>, private val newItems: List<SearchBook>) :
    DiffUtil.Callback() {

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return true
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        if (oldItem.name != newItem.name) {
            return false
        }
        if (oldItem.author != newItem.author) {
            return false
        }
        if (oldItem.kind != newItem.kind) {
            return false
        }
        if (oldItem.latestChapterTitle != newItem.latestChapterTitle) {
            return false
        }
        if (oldItem.intro != newItem.intro) {
            return false
        }
        if (oldItem.coverUrl != newItem.coverUrl) {
            return false
        }
        return true
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return when {
            oldItem.origins?.size != newItem.origins?.size -> 1
            oldItem.coverUrl != newItem.coverUrl -> 2
            oldItem.kind != newItem.kind -> 3
            oldItem.latestChapterTitle != newItem.latestChapterTitle -> 4
            oldItem.intro != newItem.intro -> 5
            else -> null
        }
    }
}