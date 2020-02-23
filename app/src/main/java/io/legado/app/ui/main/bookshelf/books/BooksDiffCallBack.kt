package io.legado.app.ui.main.bookshelf.books

import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.Book

class BooksDiffCallBack(private val oldItems: List<Book>, private val newItems: List<Book>) :
    DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldItems.size
    }

    override fun getNewListSize(): Int {
        return newItems.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition].bookUrl == newItems[newItemPosition].bookUrl
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        if (oldItem.name != newItem.name)
            return false
        if (oldItem.author != newItem.author)
            return false
        if (oldItem.durChapterTitle != newItem.durChapterTitle)
            return false
        if (oldItem.latestChapterTitle != newItem.latestChapterTitle)
            return false
        if (oldItem.lastCheckCount != newItem.lastCheckCount)
            return false
        if (oldItem.getDisplayCover() != newItem.getDisplayCover())
            return false
        if (oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum())
            return false
        return true
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        val bundle = bundleOf()
        if (oldItem.name != newItem.name)
            bundle.putString("name", null)
        if (oldItem.author != newItem.author)
            bundle.putString("author", null)
        if (oldItem.durChapterTitle != newItem.durChapterTitle)
            bundle.putString("dur", null)
        if (oldItem.latestChapterTitle != newItem.latestChapterTitle)
            bundle.putString("last", null)
        if (oldItem.getDisplayCover() != newItem.getDisplayCover())
            bundle.putString("cover", null)
        if (oldItem.lastCheckCount != newItem.lastCheckCount)
            bundle.putString("refresh", null)
        if (oldItem.getUnreadChapterNum() != newItem.getUnreadChapterNum()
            || oldItem.lastCheckCount != newItem.lastCheckCount
        ) {
            bundle.putString("refresh", null)
        }

        if (bundle.isEmpty) {
            return null
        }
        return bundle
    }

}