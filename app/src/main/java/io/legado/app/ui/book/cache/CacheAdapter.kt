package io.legado.app.ui.book.cache

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemDownloadBinding
import io.legado.app.service.help.CacheBook
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet


class CacheAdapter(context: Context, private val callBack: CallBack) :
    RecyclerAdapter<Book, ItemDownloadBinding>(context) {

    val cacheChapters = hashMapOf<String, HashSet<String>>()
    var downloadMap: ConcurrentHashMap<String, CopyOnWriteArraySet<BookChapter>>? = null

    override fun getViewBinding(parent: ViewGroup): ItemDownloadBinding {
        return ItemDownloadBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemDownloadBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        with(binding) {
            if (payloads.isEmpty()) {
                tvName.text = item.name
                tvAuthor.text = context.getString(R.string.author_show, item.getRealAuthor())
                val cs = cacheChapters[item.bookUrl]
                if (cs == null) {
                    tvDownload.setText(R.string.loading)
                } else {
                    tvDownload.text =
                        context.getString(R.string.download_count, cs.size, item.totalChapterNum)
                }
                upDownloadIv(ivDownload, item)
            } else {
                val cacheSize = cacheChapters[item.bookUrl]?.size ?: 0
                tvDownload.text =
                    context.getString(R.string.download_count, cacheSize, item.totalChapterNum)
                upDownloadIv(ivDownload, item)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemDownloadBinding) {
        with(binding) {
            ivDownload.onClick {
                getItem(holder.layoutPosition)?.let {
                    if (downloadMap?.containsKey(it.bookUrl) == true) {
                        CacheBook.remove(context, it.bookUrl)
                    } else {
                        CacheBook.start(context, it.bookUrl, 0, it.totalChapterNum)
                    }
                }
            }
            tvExport.onClick {
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