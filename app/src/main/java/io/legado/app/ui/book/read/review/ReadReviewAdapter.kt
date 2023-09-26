package io.legado.app.ui.book.read.review

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookReview
import io.legado.app.databinding.ItemReviewListBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.utils.gone
import io.legado.app.utils.visible


class ReadReviewAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookReview, ItemReviewListBinding>(context) {

    private var isLoading = false

    override fun getViewBinding(parent: ViewGroup): ItemReviewListBinding {
        return ItemReviewListBinding.inflate(inflater, parent, false)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun convert(
        holder: ItemViewHolder,
        binding: ItemReviewListBinding,
        item: BookReview,
        payloads: MutableList<Any>
    ) {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            binding.run {
                //默认图片
                val defaultUserAvatar = context.getDrawable(R.drawable.image_default_avatar)
                val defaultContentImage = context.getDrawable(R.drawable.image_loading_error)
                //加载头像
                loadImage(
                    item.reviewPostAvatar,
                    item.baseUrl,
                    AppConfig.loadCoverOnlyWifi,
                    defaultDrawable = defaultUserAvatar,
                    transformation = CircleCrop(),
                    view = ivPostAvatar
                )
                //加载内容
                tvReviewContent.text = item.reviewContent
                if (item.reviewImgUrl.isNullOrBlank()) {
                    ivReviewImgUrl.gone()
                } else {
                    ivReviewImgUrl.visible()
                    loadImage(
                        item.reviewImgUrl,
                        item.baseUrl,
                        AppConfig.loadCoverOnlyWifi,
                        defaultDrawable = defaultContentImage,
                        transformation = CenterCrop(),
                        view = ivReviewImgUrl
                    )
                }
                //其他信息
                tvPostName.text = item.reviewPostName
                tvPostTime.text = item.reviewPostTime
                tvReviewLike.text = item.reviewLikeCount
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemReviewListBinding) {
        // 图片点击
        binding.ivReviewImgUrl.setOnLongClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.onImageLongPress(it.reviewImgUrl!!)
            }
            true
        }
    }

    private val glideListener by lazy {
        object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                isLoading = true
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                isLoading = false
                return false
            }

        }
    }

    /**
     * 加载网络图片
     */
    private fun loadImage(
        path: String?,
        sourceOrigin: String? = null,
        loadOnlyWifi: Boolean = false,
        defaultDrawable: Drawable?,
        transformation: Transformation<Bitmap>,
        view: ImageView
    ) {
        val bitmapTransform = RequestOptions.bitmapTransform(transformation)
        if (path.isNullOrBlank() || isLoading) {
            ImageLoader.load(context, defaultDrawable)
                .apply(bitmapTransform)
                .into(view)
        } else {
            var options = RequestOptions().set(OkHttpModelLoader.loadOnlyWifiOption, loadOnlyWifi)
            if (sourceOrigin != null) {
                options = options.set(OkHttpModelLoader.sourceOriginOption, sourceOrigin)
            }
            //Glide自动识别http://,content://和file://
            ImageLoader.load(context, path)
                .apply(options)
                .apply(bitmapTransform)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(defaultDrawable)
                .error(defaultDrawable)
                .listener(glideListener)
                .into(view)
        }
    }

    interface CallBack {
        fun onImageLongPress(src: String)
    }
}
