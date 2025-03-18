package io.legado.app.ui.book.manga.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import io.legado.app.ui.book.manga.config.MangaColorFilterConfig
import io.legado.app.ui.book.manga.entities.MangaPage
import io.legado.app.ui.book.manga.entities.ReaderLoading
import io.legado.app.utils.getCompatDrawable


class MangaAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), PreloadModelProvider<Any> {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var mConfig: MangaColorFilterConfig

    companion object {
        private const val LOADING_VIEW = 0
        private const val CONTENT_VIEW = 1
    }

    var isHorizontal = false

    private val mDiffCallback: DiffUtil.ItemCallback<Any> = object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is ReaderLoading && newItem is ReaderLoading) {
                newItem.mMessage == oldItem.mMessage
            } else if (oldItem is MangaPage && newItem is MangaPage) {
                oldItem.mImageUrl == newItem.mImageUrl
            } else false
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return if (oldItem is ReaderLoading && newItem is ReaderLoading) {
                oldItem == newItem
            } else if (oldItem is MangaPage && newItem is MangaPage) {
                oldItem == newItem
            } else false
        }
    }

    private val mDiffer = AsyncListDiffer(this, mDiffCallback)

    fun getItem(@IntRange(from = 0) position: Int) = mDiffer.currentList.getOrNull(position)

    fun getItems() = mDiffer.currentList

    fun isEmpty() = mDiffer.currentList.isEmpty()

    fun isNotEmpty() = !isEmpty()

    //全部替换数据
    fun submitList(contents: List<Any>, runnable: Runnable? = null) {
        mDiffer.submitList(contents, runnable)
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
                if (item is MangaPage) {
                    loadImageWithRetry(
                        item.mImageUrl, isHorizontal, item.imageCount == 1
                    )
                }
            }
        }

        fun onBind(item: MangaPage) {
            setImageColorFilter()
            loadImageWithRetry(item.mImageUrl, isHorizontal, item.imageCount == 1)
        }

        fun setImageColorFilter() {
            require(
                mConfig.r in 0..255 &&
                        mConfig.g in 0..255 &&
                        mConfig.b in 0..255 &&
                        mConfig.a in 0..255
            ) {
                "ARGB values must be between 0-255"
            }
            val matrix = floatArrayOf(
                (255 - mConfig.r) / 255f, 0f, 0f, 0f, 0f,
                0f, (255 - mConfig.g) / 255f, 0f, 0f, 0f,
                0f, 0f, (255 - mConfig.b) / 255f, 0f, 0f,
                0f, 0f, 0f, (255 - mConfig.a) / 255f, 0f
            )
            binding.image.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
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

            viewType == LOADING_VIEW -> {
                PageMoreViewHolder(BookComicLoadingRvBinding.inflate(inflater, parent, false))
            }

            viewType == CONTENT_VIEW -> {
                PageViewHolder(BookComicRvBinding.inflate(inflater, parent, false))
            }

            else -> error("Unknown view type!")
        }
    }

    override fun getItemCount(): Int = getActualItemCount() + getFooterCount()

    override fun getItemViewType(position: Int): Int {
        return when {
            isFooter(position) -> TYPE_FOOTER_VIEW + position - getActualItemCount()
            getItem(position) is MangaPage -> CONTENT_VIEW
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
            is PageViewHolder -> vh.onBind(getItem(position) as MangaPage)
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
    fun getActualItemCount() = getItems().size

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

    override fun getPreloadItems(position: Int): List<Any> {
        if (isEmpty()) return emptyList()
        if (position >= getItems().size) return emptyList()
        return getItems().subList(position, position + 1)
    }

    override fun getPreloadRequestBuilder(item: Any): RequestBuilder<*>? {
        if (item is MangaPage) {
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

    @SuppressLint("NotifyDataSetChanged")
    fun setMangaImageColorFilter(config: MangaColorFilterConfig) {
        mConfig = config
        notifyItemRangeChanged(0, itemCount)
    }
}