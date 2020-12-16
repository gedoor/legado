package io.legado.app.ui.book.info

import android.content.Context
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemChapterListBinding
import io.legado.app.lib.theme.accentColor
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.textColorResource

class ChapterListAdapter(context: Context, var callBack: CallBack) :
    RecyclerAdapter<BookChapter, ItemChapterListBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemChapterListBinding {
        return ItemChapterListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChapterListBinding,
        item: BookChapter,
        payloads: MutableList<Any>
    ) {
        with(binding) {
            tvChapterName.text = item.title
            if (item.index == callBack.durChapterIndex()) {
                tvChapterName.setTextColor(context.accentColor)
            } else {
                tvChapterName.textColorResource = R.color.secondaryText
            }

        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChapterListBinding) {
        holder.itemView.apply {
            this.onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.openChapter(it)
                }
            }
        }
    }

    interface CallBack {
        fun openChapter(chapter: BookChapter)
        fun durChapterIndex(): Int
    }
}