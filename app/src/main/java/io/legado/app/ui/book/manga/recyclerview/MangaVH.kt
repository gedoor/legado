package io.legado.app.ui.book.manga.recyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.legado.app.help.glide.progress.ProgressManager
import io.legado.app.model.BookCover
import io.legado.app.model.ReadManga
import io.legado.app.utils.printOnDebug

open class MangaVH<VB : ViewBinding>(val binding: VB, private val context: Context) :
    RecyclerView.ViewHolder(binding.root) {

    protected lateinit var mLoading: ProgressBar
    protected lateinit var mImage: AppCompatImageView
    protected lateinit var mProgress: TextView
    protected lateinit var mFlProgress: FrameLayout
    protected var mRetry: Button? = null

    fun initComponent(
        loading: ProgressBar,
        image: AppCompatImageView,
        progress: TextView,
        button: Button? = null,
        flProgress: FrameLayout,
    ) {
        mLoading = loading
        mImage = image
        mRetry = button
        mProgress = progress
        mFlProgress = flProgress
    }

    @SuppressLint("CheckResult")
    fun loadImageWithRetry(imageUrl: String, isHorizontal: Boolean, singleImage: Boolean) {
        mFlProgress.isVisible = true
        mLoading.isVisible = true
        mRetry?.isGone = true
        mProgress.isVisible = true
        ProgressManager.removeListener(imageUrl)
        ProgressManager.addListener(imageUrl) { _, percentage, _, _ ->
            @SuppressLint("SetTextI18n")
            mProgress.text = "$percentage%"
        }
        try {
            mImage.tag = imageUrl
            BookCover.loadManga(
                context,
                imageUrl,
                sourceOrigin = ReadManga.book?.origin,
            ).addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    mFlProgress.isVisible = true
                    mLoading.isGone = true
                    mRetry?.isVisible = true
                    mProgress.isGone = true
                    itemView.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    mFlProgress.isGone = true
                    if (!isHorizontal) {
                        itemView.updateLayoutParams<ViewGroup.LayoutParams> {
                            height = if (singleImage) {
                                ViewGroup.LayoutParams.MATCH_PARENT
                            } else {
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }
                    } else {
                        mImage.updateLayoutParams<FrameLayout.LayoutParams> {
                            gravity = Gravity.CENTER
                        }
                    }
                    return false
                }
            }).into(mImage)
        } catch (e: Exception) {
            e.printOnDebug()
        }

    }
}