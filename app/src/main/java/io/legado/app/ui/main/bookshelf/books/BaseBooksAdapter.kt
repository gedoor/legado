package io.legado.app.ui.main.bookshelf.books

import android.content.Context
import androidx.core.os.bundleOf
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book

abstract class BaseBooksAdapter(context: Context, layoutId: Int) :
    SimpleRecyclerAdapter<Book>(context, layoutId) {

    fun notification(bookUrl: String) {
        for (i in 0 until itemCount) {
            getItem(i)?.let {
                if (it.bookUrl == bookUrl) {
                    notifyItemChanged(i, bundleOf(Pair("refresh", null)))
                    return
                }
            }
        }
    }

    interface CallBack {
        fun open(book: Book)
        fun openBookInfo(book: Book)
        fun isUpdate(bookUrl: String): Boolean
    }
}