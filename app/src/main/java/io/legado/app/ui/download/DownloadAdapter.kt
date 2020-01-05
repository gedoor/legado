package io.legado.app.ui.download

import android.content.Context
import android.view.View
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import kotlinx.android.synthetic.main.item_download.view.*


class DownloadAdapter(context: Context) :
    SimpleRecyclerAdapter<Book>(context, R.layout.item_download) {

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                tv_name.text = item.name
                tv_author.text = item.author
                upDownloadCount(this, item)
            } else {
                upDownloadCount(this, item)
            }
        }
    }

    private fun upDownloadCount(view: View, book: Book) {
        view.tv_download.text = context.getString(
            R.string.download_count,
            BookHelp.getChapterCount(book),
            book.totalChapterNum
        )
    }

}