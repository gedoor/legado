package io.legado.app.ui.main.bookshelf.books

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.Book

class BooksDiffCallBack() : DiffUtil.ItemCallback<Book>() {

    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.name == newItem.name
                && oldItem.author == newItem.author
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return when {
            oldItem.durChapterTime != newItem.durChapterTime -> false
            oldItem.name != newItem.name -> false
            oldItem.author != newItem.author -> false
            oldItem.durChapterTitle != newItem.durChapterTitle -> false
            oldItem.latestChapterTitle != newItem.latestChapterTitle -> false
            oldItem.lastCheckCount != newItem.lastCheckCount -> false
            oldItem.getDisplayCover() != newItem.getDisplayCover() -> false
            oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum() -> false
            else -> true
        }
    }

    override fun getChangePayload(oldItem: Book, newItem: Book): Any? {
        val bundle = bundleOf()
        if (oldItem.name != newItem.name) {
            bundle.putString("name", newItem.name)
        }
        if (oldItem.author != newItem.author) {
            bundle.putString("author", newItem.author)
        }
        if (oldItem.durChapterTitle != newItem.durChapterTitle) {
            bundle.putString("dur", newItem.durChapterTitle)
        }
        if (oldItem.latestChapterTitle != newItem.latestChapterTitle) {
            bundle.putString("last", newItem.latestChapterTitle)
        }
        if (oldItem.getDisplayCover() != newItem.getDisplayCover()) {
            bundle.putString("cover", newItem.getDisplayCover())
        }
        if (oldItem.lastCheckCount != newItem.lastCheckCount
            || oldItem.durChapterTime != newItem.durChapterTime
            || oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum()
            || oldItem.lastCheckCount != newItem.lastCheckCount
        ) {
            bundle.putBoolean("refresh", true)
        }
        if (bundle.isEmpty) return null
        return bundle
    }

}