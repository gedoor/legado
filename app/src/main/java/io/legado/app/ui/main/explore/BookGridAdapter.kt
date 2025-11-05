package io.legado.app.ui.main.explore

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemBookGridBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import io.legado.app.utils.dpToPx

/**
 * 书籍网格适配器
 * 用于在发现界面以网格形式展示书籍
 */
class BookGridAdapter(
    context: Context,
    private val callBack: CallBack
) : RecyclerAdapter<SearchBook, ItemBookGridBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemBookGridBinding {
        return ItemBookGridBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookGridBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            bind(binding, item)
        }
    }

    private fun bind(binding: ItemBookGridBinding, item: SearchBook) {
        binding.run {
            // 书名
            tvTitle.text = item.name
            
            // 作者
            tvAuthor.text = item.author
            
            // 封面
            ivCover.load(
                item.coverUrl,
                item.name,
                item.author,
                AppConfig.loadCoverOnlyWifi,
                item.origin
            )
            
            // 状态标签
            val status = getBookStatus(item)
            if (status.isNotBlank()) {
                tvStatus.text = status
                tvStatus.visible()
            } else {
                tvStatus.gone()
            }
            
            // 标签
            val tags = item.getKindList().take(2)
            if (tags.isNotEmpty()) {
                chipGroupTags.visible()
                // 动态设置标签
                updateTags(chipGroupTags, tags)
            } else {
                chipGroupTags.gone()
            }
        }
    }
    
    /**
     * 获取书籍状态
     */
    private fun getBookStatus(book: SearchBook): String {
        return when {
            // 检查书名中的状态信息
            book.name.contains("完结") || book.name.contains("完本") -> "完结"
            book.name.contains("连载") -> "连载"
            // 检查最新章节标题中的状态信息
            book.latestChapterTitle?.contains("完结") == true || 
            book.latestChapterTitle?.contains("完本") == true -> "完结"
            book.latestChapterTitle?.contains("连载") == true -> "连载"
            // 检查简介中的状态信息
            book.intro?.contains("完结") == true || 
            book.intro?.contains("完本") == true -> "完结"
            book.intro?.contains("连载") == true -> "连载"
            // 根据章节数量判断（如果章节数很多可能是完结的）
            (book.getKindList().size > 3) -> "完结"
            else -> "连载"
        }
    }
    
    /**
     * 更新标签显示
     */
    private fun updateTags(chipGroup: com.google.android.material.chip.ChipGroup, tags: List<String>) {
        // 移除多余的标签
        while (chipGroup.childCount > tags.size) {
            chipGroup.removeViewAt(chipGroup.childCount - 1)
        }
        
        // 更新或添加标签
        tags.forEachIndexed { index, tag ->
            val chip = if (index < chipGroup.childCount) {
                chipGroup.getChildAt(index) as com.google.android.material.chip.Chip
            } else {
                val newChip = com.google.android.material.chip.Chip(context)
                newChip.apply {
                    setChipBackgroundColorResource(
                        if (index % 2 == 0) R.color.md_blue_100 
                        else R.color.md_green_100
                    )
                    setTextColor(context.getColor(R.color.md_blue_900))
                    chipMinHeight = 20.dpToPx().toFloat()
                    chipStrokeWidth = 0f
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 8f)
                }
                chipGroup.addView(newChip)
                newChip
            }
            chip.text = tag
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookGridBinding) {
        binding.root.setOnClickListener {
            getItem(holder.layoutPosition)?.let { book ->
                callBack.onBookClick(book)
            }
        }
        
        binding.root.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let { book ->
                callBack.onBookLongClick(book)
            }
            true
        }
    }

    interface CallBack {
        fun onBookClick(book: SearchBook)
        fun onBookLongClick(book: SearchBook)
    }
}
