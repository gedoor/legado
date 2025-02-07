package io.legado.app.ui.book.manga.rv

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.IntRange
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter.Companion.TYPE_FOOTER_VIEW
import io.legado.app.databinding.BookComicLoadingRvBinding
import io.legado.app.databinding.BookComicRvBinding
import io.legado.app.model.ReadMange
import io.legado.app.model.recyclerView.MangaVH
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.ReaderLoading


class MangaAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val LOADING_VIEW = 0
        private const val CONTENT_VIEW = 1
    }

    private val mDiffCallback: DiffUtil.ItemCallback<Any> = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is ReaderLoading && newItem is ReaderLoading) {
                newItem.mMessage == oldItem.mMessage
            } else if (oldItem is MangeContent && newItem is MangeContent) {
                oldItem.mImageUrl == newItem.mImageUrl
            } else false
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is ReaderLoading && newItem is ReaderLoading) {
                oldItem == newItem
            } else if (oldItem is MangeContent && newItem is MangeContent) {
                oldItem == newItem
            } else false
        }
    }

    private val mDiffer = AsyncListDiffer(this, mDiffCallback)

    private fun getItem(@IntRange(from = 0) position: Int) = mDiffer.currentList[position]

    fun getCurrentList() = mDiffer.currentList

    //全部替换数据
    fun submitList(contents: MutableList<Any>, runnable: Runnable) {
        val currentList = mDiffer.currentList.toMutableList()
        currentList.addAll(contents)
        if (ReadMange.chapterChanged) {
            mDiffer.submitList(contents) {
                runnable.run()
            }
        } else {
            mDiffer.submitList(currentList) {
                runnable.run()
            }
        }

    }

    inner class PageViewHolder(binding: BookComicRvBinding) :
        MangaVH<BookComicRvBinding>(binding) {

        init {
            initComponent(binding.loading, binding.image, binding.progress, binding.retry)
            binding.retry.setOnClickListener {
                val item = mDiffer.currentList[layoutPosition]
                if (item is MangeContent) {
                    binding.image.recycle()
                    loadImageWithRetry(item.mImageUrl)
                }
            }
        }

        fun onBind(item: MangeContent) {
            loadImageWithRetry(item.mImageUrl)
        }
    }

    inner class PageMoreViewHolder(val binding: BookComicLoadingRvBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(item: ReaderLoading) {
            val message = item.mMessage
            binding.text.text = message
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when {

            viewType >= TYPE_FOOTER_VIEW -> {
                ItemViewHolder(footerItems.get(viewType).invoke(parent))
            }

            viewType == LOADING_VIEW -> PageMoreViewHolder(
                BookComicLoadingRvBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

            viewType == CONTENT_VIEW -> PageViewHolder(
                BookComicRvBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )


            else -> error("Unknown view type!")
        }
    }

    override fun getItemCount(): Int = getActualItemCount() + getFooterCount()

    override fun getItemViewType(position: Int): Int {
        return when {
            isFooter(position) -> TYPE_FOOTER_VIEW + position - getActualItemCount()
            getItem(position) is MangeContent -> CONTENT_VIEW
            getItem(position) is ReaderLoading -> LOADING_VIEW
            else -> error("Unknown view type!")
        }
    }

    fun getFooterCount() = footerItems.size()

    private fun isFooter(position: Int) = position >= getActualItemCount()

    override fun onViewRecycled(vh: RecyclerView.ViewHolder) {
        super.onViewRecycled(vh)
        when (vh) {
            is PageViewHolder -> {
                vh.binding.image.recycle()
                vh.itemView.updateLayoutParams<ViewGroup.LayoutParams> { height = MATCH_PARENT }
                Glide.with(vh.binding.image).clear(vh.binding.image)
            }
        }
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        when (vh) {
            is PageViewHolder -> vh.onBind(getItem(position) as MangeContent)
            is PageMoreViewHolder -> vh.onBind(getItem(position) as ReaderLoading)
        }
    }


    private val footerItems: SparseArray<(parent: ViewGroup) -> ViewBinding> by lazy { SparseArray() }

    @Synchronized
    fun addFooterView(footer: ((parent: ViewGroup) -> ViewBinding)) {
        kotlin.runCatching {
            val index = getActualItemCount() + footerItems.size()
            footerItems.put(TYPE_FOOTER_VIEW + footerItems.size(), footer)
            notifyItemInserted(index)
        }
    }

    /**
     * 除去header和footer
     */
    fun getActualItemCount() = getCurrentList().size

    @Synchronized
    fun removeFooterView(footer: ((parent: ViewGroup) -> ViewBinding)) {
        kotlin.runCatching {
            val index = footerItems.indexOfValue(footer)
            if (index >= 0) {
                footerItems.remove(index)
                notifyItemRemoved(getActualItemCount() + index - 2)
            }
        }
    }
}