package io.legado.app.ui.book.toc

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemChapterListBinding
import io.legado.app.help.ContentProcessor
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.theme.ThemeUtils
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap

class ChapterListAdapter(context: Context, val callback: Callback) :
    RecyclerAdapter<BookChapter, ItemChapterListBinding>(context) {

    val cacheFileNames = hashSetOf<String>()
    val displayTileMap = ConcurrentHashMap<Int, String>()
    val diffCallBack = object : DiffUtil.ItemCallback<BookChapter>() {

        override fun areItemsTheSame(
            oldItem: BookChapter,
            newItem: BookChapter
        ): Boolean {
            return oldItem.index == newItem.index
        }

        override fun areContentsTheSame(
            oldItem: BookChapter,
            newItem: BookChapter
        ): Boolean {
            return oldItem.bookUrl == newItem.bookUrl
                && oldItem.url == newItem.url
                && oldItem.isVip == newItem.isVip
                && oldItem.isPay == newItem.isPay
                && oldItem.title == newItem.title
                && oldItem.tag == newItem.tag
                && oldItem.isVolume == newItem.isVolume
        }

    }

    private val replaceRules
        get() = callback.book?.let {
            ContentProcessor.get(it.name, it.origin).getTitleReplaceRules()
        }
    private val useReplace
        get() = AppConfig.tocUiUseReplace && callback.book?.getUseReplaceRule() == true
    private var upDisplayTileJob: Coroutine<*>? = null

    fun upDisplayTile() {
        upDisplayTileJob?.cancel()
        upDisplayTileJob = Coroutine.async(callback.scope) {
            val replaceRules = replaceRules
            val useReplace = useReplace
            getItems().forEach {
                if (!isActive) {
                    return@async
                }
                if (displayTileMap[it.index] == null) {
                    displayTileMap[it.index] = it.getDisplayTitle(replaceRules, useReplace)
                }
            }
        }
    }

    override fun getViewBinding(parent: ViewGroup): ItemChapterListBinding {
        return ItemChapterListBinding.inflate(inflater, parent, false)
    }

    private fun getDisplayTile(chapter: BookChapter): String {
        var displayTile = displayTileMap[chapter.index]
        if (displayTile != null) {
            return displayTile
        }
        displayTile = chapter.getDisplayTitle(replaceRules, useReplace)
        displayTileMap[chapter.index] = displayTile
        return displayTile
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChapterListBinding,
        item: BookChapter,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val isDur = callback.durChapterIndex() == item.index
            val cached = callback.isLocalBook || cacheFileNames.contains(item.getFileName())
            if (payloads.isEmpty()) {
                if (isDur) {
                    tvChapterName.setTextColor(context.accentColor)
                } else {
                    tvChapterName.setTextColor(context.getCompatColor(R.color.primaryText))
                }
                tvChapterName.text = getDisplayTile(item)
                if (item.isVolume) {
                    //卷名，如第一卷 突出显示
                    tvChapterItem.setBackgroundColor(context.getCompatColor(R.color.btn_bg_press))
                } else {
                    //普通章节 保持不变
                    tvChapterItem.background =
                        ThemeUtils.resolveDrawable(context, android.R.attr.selectableItemBackground)
                }
                if (!item.tag.isNullOrEmpty() && !item.isVolume) {
                    //卷名不显示tag(更新时间规则)
                    tvTag.text = item.tag
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
                callback.openChapter(it)
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
        val book: Book?
        val isLocalBook: Boolean
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
    }
}