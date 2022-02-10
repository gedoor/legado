package io.legado.app.ui.book.toc

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemChapterListBinding
import io.legado.app.lib.theme.ThemeUtils
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch

class ChapterListAdapter(context: Context, val callback: Callback) :
    RecyclerAdapter<Pair<BookChapter, Deferred<String>>, ItemChapterListBinding>(context) {

    val cacheFileNames = hashSetOf<String>()
    val diffCallBack = object : DiffUtil.ItemCallback<Pair<BookChapter, Deferred<String>>>() {

        override fun areItemsTheSame(
            oldItem: Pair<BookChapter, Deferred<String>>,
            newItem: Pair<BookChapter, Deferred<String>>
        ): Boolean {
            return oldItem.first.index == newItem.first.index
        }

        override fun areContentsTheSame(
            oldItem: Pair<BookChapter, Deferred<String>>,
            newItem: Pair<BookChapter, Deferred<String>>
        ): Boolean {
            return oldItem.first.bookUrl == newItem.first.bookUrl
                && oldItem.first.url == newItem.first.url
                && oldItem.first.isVip == newItem.first.isVip
                && oldItem.first.isPay == newItem.first.isPay
                && oldItem.first.title == newItem.first.title
                && oldItem.first.tag == newItem.first.tag
                && oldItem.first.isVolume == newItem.first.isVolume
        }

    }

    override fun getViewBinding(parent: ViewGroup): ItemChapterListBinding {
        return ItemChapterListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChapterListBinding,
        item: Pair<BookChapter, Deferred<String>>,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val isDur = callback.durChapterIndex() == item.first.index
            val cached = callback.isLocalBook || cacheFileNames.contains(item.first.getFileName())
            if (payloads.isEmpty()) {
                if (isDur) {
                    tvChapterName.setTextColor(context.accentColor)
                } else {
                    tvChapterName.setTextColor(context.getCompatColor(R.color.primaryText))
                }
                callback.scope.launch {
                    tvChapterName.text = item.second.await()
                }
                if (item.first.isVolume) {
                    //卷名，如第一卷 突出显示
                    tvChapterItem.setBackgroundColor(context.getCompatColor(R.color.btn_bg_press))
                } else {
                    //普通章节 保持不变
                    tvChapterItem.background =
                        ThemeUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
                }
                if (!item.first.tag.isNullOrEmpty() && !item.first.isVolume) {
                    //卷名不显示tag(更新时间规则)
                    tvTag.text = item.first.tag
                    tvTag.visible()
                } else {
                    tvTag.gone()
                }
                upHasCache(binding, isDur, cached)
            } else {
                upHasCache(binding, isDur, cached)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChapterListBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callback.openChapter(it.first)
            }
        }
    }

    private fun upHasCache(binding: ItemChapterListBinding, isDur: Boolean, cached: Boolean) =
        binding.apply {
            ivChecked.setImageResource(R.drawable.ic_outline_cloud_24)
            ivChecked.visible(!cached)
            if (isDur) {
                ivChecked.setImageResource(R.drawable.ic_check)
                ivChecked.visible()
            }
        }

    interface Callback {
        val scope: CoroutineScope
        val isLocalBook: Boolean
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
    }
}