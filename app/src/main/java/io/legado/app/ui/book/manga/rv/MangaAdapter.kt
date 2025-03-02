package io.legado.app.ui.book.manga.rv

import android.content.Context
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
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter.Companion.TYPE_FOOTER_VIEW
import io.legado.app.databinding.BookComicLoadingRvBinding
import io.legado.app.databinding.BookComicRvBinding
import io.legado.app.help.glide.progress.ProgressManager
import io.legado.app.model.BookCover
import io.legado.app.model.ReadManga
import io.legado.app.model.recyclerView.MangaContent
import io.legado.app.model.recyclerView.MangaVH
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.utils.getCompatDrawable
import java.util.Collections


class MangaAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), PreloadModelProvider<Any> {

    companion object {
        private const val LOADING_VIEW = 0
        private const val CONTENT_VIEW = 1
    }

    var isHorizontal = false

    private val mDiffCallback: DiffUtil.ItemCallback<Any> = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is ReaderLoading && newItem is ReaderLoading) {
                newItem.mMessage == oldItem.mMessage
            } else if (oldItem is MangaContent && newItem is MangaContent) {
                oldItem.mImageUrl == newItem.mImageUrl
            } else false
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is ReaderLoading && newItem is ReaderLoading) {
                oldItem == newItem
            } else if (oldItem is MangaContent && newItem is MangaContent) {
                oldItem == newItem
            } else false
        }
    }

    private val mDiffer = AsyncListDiffer(this, mDiffCallback)

    private fun getItem(@IntRange(from = 0) position: Int) = mDiffer.currentList[position]

    fun getCurrentList() = mDiffer.currentList

    //全部替换数据
    fun submitList(contents: MutableList<Any>, runnable: Runnable) {
        val list = if (ReadManga.chapterChanged) {
            contents
        } else {
            val currentList = mDiffer.currentList.toMutableList()
            currentList.addAll(contents)
            currentList
        }
        mDiffer.submitList(list) {
            runnable.run()
        }
    }

    inner class PageViewHolder(binding: BookComicRvBinding) :
        MangaVH<BookComicRvBinding>(binding, context) {

        init {
            initComponent(
                binding.loading,
                binding.image,
                binding.progress,
                binding.retry,
                binding.flProgress
            )
            binding.retry.setOnClickListener {
                val item = mDiffer.currentList[layoutPosition]
                if (item is MangaContent) {
                    loadImageWithRetry(item.mImageUrl, isHorizontal)
                }
            }
        }

        fun onBind(item: MangaContent) {
            loadImageWithRetry(item.mImageUrl, isHorizontal)
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
            getItem(position) is MangaContent -> CONTENT_VIEW
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
                vh.itemView.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = MATCH_PARENT
                }
                Glide.with(context).clear(vh.binding.image)
                if (vh.binding.image.tag is String) {
                    ProgressManager.removeListener(vh.binding.image.tag as String)
                }
            }
        }
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        when (vh) {
            is PageViewHolder -> vh.onBind(getItem(position) as MangaContent)
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

    override fun getPreloadItems(position: Int): MutableList<Any> {
        if (getCurrentList().isEmpty()) return Collections.emptyList()
        if (position >= getCurrentList().size) return Collections.emptyList()
        return getCurrentList().subList(position, position + 1)
    }

    override fun getPreloadRequestBuilder(item: Any): RequestBuilder<*>? {
        if (item is MangaContent) {
            return BookCover.loadManga(
                context,
                item.mImageUrl,
                sourceOrigin = ReadManga.book?.origin,
                manga = true,
                useDefaultCover = context.getCompatDrawable(R.color.book_ant_10)
            )
        }
        return null
    }
}