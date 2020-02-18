package io.legado.app.ui.download

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import kotlinx.android.synthetic.main.item_download.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast


class DownloadAdapter(context: Context, private val callBack: CallBack) :
    SimpleRecyclerAdapter<Book>(context, R.layout.item_download) {

    val cacheChapters = hashMapOf<String, HashSet<String>>()
    var downloadMap: HashMap<String, LinkedHashSet<BookChapter>>? = null

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                tv_name.text = item.name
                tv_author.text = context.getString(R.string.author_show, item.getRealAuthor())
                val cs = cacheChapters[item.bookUrl]
                if (cs == null) {
                    tv_download.setText(R.string.loading)
                } else {
                    tv_download.text =
                        context.getString(R.string.download_count, cs.size, item.totalChapterNum)
                }
            } else {
                val cacheSize = cacheChapters[item.bookUrl]?.size ?: 0
                tv_download.text =
                    context.getString(R.string.download_count, cacheSize, item.totalChapterNum)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.tv_export.onClick {
            getItem(holder.layoutPosition)?.let {
                val cacheSize = cacheChapters[it.bookUrl]?.size ?: 0
                if (cacheSize < it.totalChapterNum) {
                    context.toast("未下载完成")
                } else {
                    callBack.export(holder.layoutPosition)
                }
            }
        }
    }

    interface CallBack {
        fun export(position: Int)
    }
}