package io.legado.app.ui.book.manga.rv

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewPropertyAnimator
import androidx.annotation.IntRange
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.databinding.BookComicLoadingRvBinding
import io.legado.app.databinding.BookComicRvBinding
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.MangeVH
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.utils.animateFadeIn
import io.legado.app.utils.animateFadeOutGone


class MangeAdapter(val onRetry: (nextIndex: Int, isNext: Boolean) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val LOADING_VIEW = 0
        private const val CONTENT_VIEW = 1
    }


    private val mList = mutableListOf<Any>()

    private fun getItem(@IntRange(from = 0) position: Int) = mList[position]

    fun getCurrentList() = mList

    fun submitList(contents: MutableList<Any>) {
        val oldPos = mList.size
        mList.addAll(contents)
        notifyItemRangeChanged(oldPos, mList.size)
    }

    inner class PageViewHolder(binding: BookComicRvBinding) :
        MangeVH<BookComicRvBinding>(binding) {

        init {
            initComponent(binding.loading, binding.image, binding.progress, binding.retry)
            binding.retry.setOnClickListener {
                val item = mList[layoutPosition]
                if (item is MangeContent) {
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

        private var mRetryAnimator: ViewPropertyAnimator? = null
        private var mLoadingAnimator: ViewPropertyAnimator? = null

        init {
            binding.retry.setOnClickListener {
                (getItem(absoluteAdapterPosition) as ReaderLoading).apply {
                    mRetryAnimator?.cancel()
                    mLoadingAnimator?.cancel()
                    mRetryAnimator = binding.retry.animateFadeOutGone()
                    mLoadingAnimator = binding.loading.animateFadeIn()
                    val isNext = mLoadNext == true
                    if (isNext) {
                        onRetry(mNextIndex, isNext)
                    }
                }
            }
        }

        fun onBind(item: ReaderLoading) {
            val message = item.mMessage
            if (message == null) {
                if (binding.retry.isGone) {
                    mRetryAnimator?.cancel()
                    mRetryAnimator = binding.retry.animateFadeIn()
                }
                binding.text.text = null
            } else {
                if (binding.retry.isVisible) {
                    mRetryAnimator?.cancel()
                    mRetryAnimator = binding.retry.animateFadeOutGone()
                }
                binding.text.text = message
            }
            if (binding.loading.isVisible) {
                mLoadingAnimator?.cancel()
                mLoadingAnimator = binding.loading.animateFadeOutGone()
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            LOADING_VIEW -> PageMoreViewHolder(
                BookComicLoadingRvBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

            CONTENT_VIEW -> PageViewHolder(
                BookComicRvBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

            else -> error("Unknown view type!")
        }
    }

    override fun getItemCount(): Int = mList.size

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MangeContent -> CONTENT_VIEW
            is ReaderLoading -> LOADING_VIEW
            else -> error("Unknown view type!")
        }
    }

    override fun onViewRecycled(vh: RecyclerView.ViewHolder) {
        super.onViewRecycled(vh)
        when (vh) {
            is PageViewHolder -> {
                vh.itemView.updateLayoutParams<ViewGroup.LayoutParams> { height = MATCH_PARENT }
            }
        }
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {
        when (vh) {
            is PageViewHolder -> vh.onBind(getItem(position) as MangeContent)
            is PageMoreViewHolder -> vh.onBind(getItem(position) as ReaderLoading)
        }
    }
}