package io.legado.app.ui.download

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import kotlinx.android.synthetic.main.item_download.view.*


class DownloadAdapter(context: Context) :
    SimpleRecyclerAdapter<Book>(context, R.layout.item_download) {

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                tv_name.text = item.name
                tv_author.text = context.getString(R.string.author_show, item.getRealAuthor())
                tv_download.setText(R.string.loading)
            } else {
                val count = payloads[0] as Int
                tv_download.text =
                    context.getString(R.string.download_count, count, item.totalChapterNum)
            }
        }
    }

}