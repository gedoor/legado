package io.legado.app.ui.book.search

import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.SearchBook

class DiffCallBack : DiffUtil.ItemCallback<SearchBook>() {
    override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
        return oldItem.name == newItem.name
                && oldItem.author == newItem.author
    }

    override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
        return oldItem.origins?.size == newItem.origins?.size
                && (oldItem.coverUrl == newItem.coverUrl || !oldItem.coverUrl.isNullOrEmpty())
                && (oldItem.kind == newItem.kind || !oldItem.kind.isNullOrEmpty())
                && (oldItem.latestChapterTitle == newItem.latestChapterTitle || !oldItem.kind.isNullOrEmpty())
                && oldItem.intro?.length ?: 0 > newItem.intro?.length ?: 0
    }

    override fun getChangePayload(oldItem: SearchBook, newItem: SearchBook): Any? {
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