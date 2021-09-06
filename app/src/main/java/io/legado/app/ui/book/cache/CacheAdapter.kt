package io.legado.app.ui.book.cache

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemDownloadBinding
import io.legado.app.model.CacheBook

class CacheAdapter(context: Context, private val callBack: CallBack) :
    RecyclerAdapter<Book, ItemDownloadBinding>(context) {

    val cacheChapters = hashMapOf<String, HashSet<String>>()

    override fun getViewBinding(parent: ViewGroup): ItemDownloadBinding {
        return ItemDownloadBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemDownloadBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        binding.run {
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
        binding.run {
            ivDownload.setOnClickListener {
                getItem(holder.layoutPosition)?.let { book ->
                    CacheBook.cacheBookMap[book.bookUrl]?.let {
                        if (it.isRun()) {
                            CacheBook.remove(context, book.bookUrl)
                        } else {
                            CacheBook.start(context, book.bookUrl, 0, book.totalChapterNum)
                        }
                    } ?: let {
                        CacheBook.start(context, book.bookUrl, 0, book.totalChapterNum)
                    }
                }
            }
            tvExport.setOnClickListener {
                callBack.export(holder.layoutPosition)
            }
        }
    }

    private fun upDownloadIv(iv: ImageView, book: Book) {
        CacheBook.cacheBookMap[book.bookUrl]?.let {
            if (it.isRun()) {
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