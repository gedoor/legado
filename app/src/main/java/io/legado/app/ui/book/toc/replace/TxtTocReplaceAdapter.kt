package io.legado.app.ui.book.toc.replace

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemTxtTocReplaceBinding
import io.legado.app.lib.theme.accentColor

class TxtTocReplaceAdapter(context: Context) :
    RecyclerAdapter<Pair<BookChapter, BookChapter>, ItemTxtTocReplaceBinding>(context) {

    val diffItemCallBack = object : DiffUtil.ItemCallback<Pair<BookChapter, BookChapter>>() {
        override fun areItemsTheSame(
            oldItem: Pair<BookChapter, BookChapter>,
            newItem: Pair<BookChapter, BookChapter>
        ): Boolean {
            return oldItem.first.url == newItem.first.url
        }

        override fun areContentsTheSame(
            oldItem: Pair<BookChapter, BookChapter>,
            newItem: Pair<BookChapter, BookChapter>
        ): Boolean {
            return oldItem.first.title == newItem.first.title &&
                    oldItem.second.title == newItem.second.title
        }

        override fun getChangePayload(
            oldItem: Pair<BookChapter, BookChapter>,
            newItem: Pair<BookChapter, BookChapter>
        ): Any? {
            val payload = Bundle()
            if (oldItem.first.title != newItem.first.title) {
                payload.putBoolean("updateOriginal", true)
            }
            if (oldItem.second.title != newItem.second.title) {
                payload.putBoolean("updateReplacement", true)
            }
            return if (payload.isEmpty) null else payload
        }
    }

    override fun getViewBinding(parent: ViewGroup): ItemTxtTocReplaceBinding {
        return ItemTxtTocReplaceBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemTxtTocReplaceBinding,
        item: Pair<BookChapter, BookChapter>,
        payloads: MutableList<Any>
    ) {
        binding.apply {
            replacementText.setTextColor(context.accentColor)

            if (payloads.isEmpty()) {
                originalText.text = item.first.title
                replacementText.text = item.second.title
            } else {
                for (payload in payloads) {
                    if (payload is Bundle) {
                        if (payload.getBoolean("updateOriginal")) {
                            originalText.text = item.first.title
                        }
                        if (payload.getBoolean("updateReplacement")) {
                            replacementText.text = item.second.title
                        }
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemTxtTocReplaceBinding) {

    }
}