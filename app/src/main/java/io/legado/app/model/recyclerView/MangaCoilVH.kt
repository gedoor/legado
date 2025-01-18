package io.legado.app.model.recyclerView

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import io.legado.app.help.glide.ImageLoader

open class MangaCoilVH<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root) {

    protected lateinit var mLoading: ProgressBar
    protected lateinit var mImage: SubsamplingScaleImageView
    protected var mRetry: Button? = null

    fun initComponent(
        loading: ProgressBar,
        image: SubsamplingScaleImageView,
        button: Button? = null,
    ) {
        mLoading = loading
        mImage = image
        mRetry = button
    }

    @SuppressLint("CheckResult")
    fun loadImageWithRetry(imageUrl: String) {
        mLoading.isVisible = true
        mRetry?.isGone = true
        val isNull = itemView.tag == null
        if (isNull) {
            itemView.tag = itemView
            itemView.post {
                itemView.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = MATCH_PARENT
                }
            }
        } else {
            itemView.updateLayoutParams<ViewGroup.LayoutParams> { height = MATCH_PARENT }
        }
        ImageLoader.loadBitmap(context = itemView.context, imageUrl).apply {
            addListener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>,
                    isFirstResource: Boolean,
                ): Boolean {
                    mLoading.isGone = true
                    mRetry?.isVisible = true
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: Target<Bitmap>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    mLoading.isGone = true
                    mRetry?.isGone = true
                    if (isNull) {
                        itemView.post {
                            itemView.updateLayoutParams<ViewGroup.LayoutParams> {
                                height = WRAP_CONTENT
                            }
                        }
                    } else {
                        itemView.updateLayoutParams<ViewGroup.LayoutParams> {
                            height = WRAP_CONTENT
                        }
                    }
                    return false
                }
            })
        }.into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                with(mImage) {
                    setMaxTileSize(2048)
                    setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
                    setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                    setMinimumTileDpi(180)
                    setImage(ImageSource.bitmap(resource))
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }

        })
    }

    fun loadCoverImage(imageUrl: String) {
        mLoading.isInvisible = false
    }
}