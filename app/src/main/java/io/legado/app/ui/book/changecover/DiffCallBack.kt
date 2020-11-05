package io.legado.app.ui.book.changecover

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.SearchBook

class DiffCallBack(private val oldItems: List<SearchBook>, private val newItems: List<SearchBook>) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.bookUrl == newItem.bookUrl
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        if (oldItem.originName != newItem.originName) {
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
        val payload = Bundle()
        if (oldItem.originName != newItem.originName) {
            payload.putString("name", newItem.originName)
        }
        if (oldItem.coverUrl != newItem.coverUrl) {
            payload.putString("coverUrl", newItem.coverUrl)
        }
        if (payload.isEmpty) {
            return null
        }
        return payload
    }

}