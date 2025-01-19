package io.legado.app.model.recyclerView

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import io.legado.app.help.glide.progress.OnProgressListener
import io.legado.app.help.glide.progress.ProgressManager
import io.legado.app.utils.printOnDebug
import java.io.File

open class MangeVH<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root) {

    protected lateinit var mLoading: ProgressBar
    protected lateinit var mImage: SubsamplingScaleImageView
    protected lateinit var mProgress: TextView
    protected var mRetry: Button? = null

    fun initComponent(
        loading: ProgressBar,
        image: SubsamplingScaleImageView,
        progress: TextView,
        button: Button? = null,
    ) {
        mLoading = loading
        mImage = image
        mRetry = button
        mProgress = progress
    }

    @SuppressLint("CheckResult")
    fun loadImageWithRetry(imageUrl: String) {
        mLoading.isVisible = true
        mRetry?.isGone = true
        mProgress.isVisible = true
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
        mImage.recycle()
        ProgressManager.removeListener(imageUrl)
        ProgressManager.addListener(imageUrl, object : OnProgressListener {
            @SuppressLint("SetTextI18n")
            override fun invoke(
                isComplete: Boolean,
                percentage: Int,
                bytesRead: Long,
                totalBytes: Long,
            ) {
                mProgress.text = "${percentage}%"
            }
        })
        try {
            Glide.with(itemView.context)
                .download(imageUrl)
                .addListener(object : RequestListener<File> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<File>,
                        isFirstResource: Boolean,
                    ): Boolean {
                        mLoading.isGone = true
                        mRetry?.isVisible = true
                        mProgress.isGone = true
                        return false
                    }

                    override fun onResourceReady(
                        resource: File,
                        model: Any,
                        target: Target<File>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean,
                    ): Boolean {
                        mLoading.isGone = true
                        mRetry?.isGone = true
                        mProgress.isGone = true
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
                }).into(object : CustomTarget<File>() {
                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        with(mImage) {
                            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
                            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                            setMinimumTileDpi(180)
                            setOnImageEventListener(
                                object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                                    override fun onReady() {
                                        mLoading.isGone = true
                                        mRetry?.isGone = true
                                    }

                                    override fun onImageLoadError(e: Exception) {
                                        mLoading.isGone = true
                                        mRetry?.isVisible = true
                                    }
                                },
                            )
                            setImage(ImageSource.uri(itemView.context, Uri.fromFile(resource)))
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        mImage.recycle()
                    }

                })
        } catch (e: Exception) {
            e.printOnDebug()
        }

    }

    fun loadCoverImage(imageUrl: String) {
        mLoading.isInvisible = false
    }
}