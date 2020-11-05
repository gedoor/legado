package io.legado.app.ui.book.search

import android.os.Bundle
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
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return when {
            oldItem.name != newItem.name -> false
            oldItem.author != newItem.author -> false
            else -> true
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return when {
            oldItem.origins.size != newItem.origins.size -> false
            oldItem.coverUrl != newItem.coverUrl -> false
            oldItem.kind != newItem.kind -> false
            oldItem.latestChapterTitle != newItem.latestChapterTitle -> false
            oldItem.intro != newItem.intro -> false
            else -> true
        }
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val payload = Bundle()
        val newItem = newItems[newItemPosition]
        val oldItem = oldItems[oldItemPosition]
        if (oldItem.name != newItem.name) payload.putString("name", newItem.name)
        if (oldItem.author != newItem.author) payload.putString("author", newItem.author)
        if (oldItem.origins.size != newItem.origins.size)
            payload.putInt("origins", newItem.origins.size)
        if (oldItem.coverUrl != newItem.coverUrl) payload.putString("cover", newItem.coverUrl)
        if (oldItem.kind != newItem.kind) payload.putString("kind", newItem.kind)
        if (oldItem.latestChapterTitle != newItem.latestChapterTitle)
            payload.putString("last", newItem.latestChapterTitle)
        if (oldItem.intro != newItem.intro) payload.putString("intro", newItem.intro)
        if (payload.isEmpty) return null
        return payload
    }
}