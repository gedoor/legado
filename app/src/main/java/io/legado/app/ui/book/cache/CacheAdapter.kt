package io.legado.app.ui.book.cache

import android.content.Context
import android.widget.ImageView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.service.help.CacheBook
import kotlinx.android.synthetic.main.item_download.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet


class CacheAdapter(context: Context, private val callBack: CallBack) :
    SimpleRecyclerAdapter<Book>(context, R.layout.item_download) {

    val cacheChapters = hashMapOf<String, HashSet<String>>()
    var downloadMap: ConcurrentHashMap<String, CopyOnWriteArraySet<BookChapter>>? = null

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
                upDownloadIv(iv_download, item)
            } else {
                val cacheSize = cacheChapters[item.bookUrl]?.size ?: 0
                tv_download.text =
                    context.getString(R.string.download_count, cacheSize, item.totalChapterNum)
                upDownloadIv(iv_download, item)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            iv_download.onClick {
                getItem(holder.layoutPosition)?.let {
                    if (downloadMap?.containsKey(it.bookUrl) == true) {
                        CacheBook.remove(context, it.bookUrl)
                    } else {
                        CacheBook.start(context, it.bookUrl, 0, it.totalChapterNum)
                    }
                }
            }
            tv_export.onClick {
                callBack.export(holder.layoutPosition)
            }
        }
    }

    private fun upDownloadIv(iv: ImageView, book: Book) {
        downloadMap?.let {
            if (it.containsKey(book.bookUrl)) {
                iv.setImageResource(R.drawable.ic_stop_black_24dp)
            } else {
                iv.setImageResource(R.drawable.ic_play_24dp)
            }
        } ?: let {
            iv.setImageResource(R.drawable.ic_play_24dp)
        }
    }

    interface CallBack {
        fun export(position: Int)
    }
}