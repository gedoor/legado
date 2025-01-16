package io.legado.app.model.recyclerView

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ImageView
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
import com.bumptech.glide.request.target.Target
import io.legado.app.help.glide.ImageLoader

open class MangaCoilVH<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root) {

    protected lateinit var mLoading: ProgressBar
    protected lateinit var mImage: ImageView
    protected var mRetry: Button? = null

    fun initComponent(
        loading: ProgressBar,
        image: ImageView,
        button: Button? = null
    ) {
        mLoading = loading
        mImage = image
        mRetry = button
    }

    @SuppressLint("CheckResult")
    fun loadImageWithRetry(imageUrl: String) {
        mLoading.isInvisible = false
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
        ImageLoader.load(context = itemView.context, imageUrl).apply {
            addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    mLoading.isInvisible = true
                    mRetry?.isVisible = true
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    mLoading.isInvisible = true
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
                    /*itemView.post {
                        val imageWidth: Int = resource.intrinsicWidth
                        val imageHeight: Int = resource.intrinsicHeight
                        val viewWidth: Int = itemView.width
                        val imageViewHeight = (viewWidth * imageHeight) / imageWidth
                        itemView.layoutParams.width = viewWidth
                        itemView.layoutParams.height = imageViewHeight
                        itemView.updateLayoutParams<ViewGroup.LayoutParams> {
                            width = viewWidth
                            height = imageViewHeight
                        }
                    }*/
                    return false
                }

            })
        }.into(mImage)
    }

    fun loadCoverImage(imageUrl: String) {
        mLoading.isInvisible = false
    }
}