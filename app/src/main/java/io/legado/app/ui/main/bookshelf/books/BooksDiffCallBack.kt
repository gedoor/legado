package io.legado.app.ui.main.bookshelf.books

import androidx.recyclerview.widget.DiffUtil
import io.legado.app.data.entities.Book

class BooksDiffCallBack(private val oldItems: List<Book>, private val newItems: List<Book>) :
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
        val oldItem = oldItems[oldItemPosition]
        val newItem = newItems[newItemPosition]
        return oldItem.name == newItem.name
                && oldItem.author == newItem.author
                && oldItem.durChapterTitle == newItem.durChapterTitle
                && oldItem.latestChapterTitle == newItem.latestChapterTitle
                && oldItem.getDisplayCover() == newItem.getDisplayCover()
                && oldItem.getUnreadChapterNum() == newItem.getUnreadChapterNum()
                && oldItem.lastCheckCount == newItem.lastCheckCount
    }

}