package io.legado.app.ui.rss.article

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.RssArticle
import io.legado.app.databinding.ItemRssArticle3Binding
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.utils.getCompatColor
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import com.bumptech.glide.request.target.Target

class RssArticlesAdapter3(context: Context, callBack: CallBack) :
    BaseRssArticlesAdapter<ItemRssArticle3Binding>(context, callBack) {

    companion object {
        val imageSizeCache = LruCache<String, Pair<Int, Int>>(999)
    }
    override fun getViewBinding(parent: ViewGroup): ItemRssArticle3Binding {
        return ItemRssArticle3Binding.inflate(inflater, parent, false)
    }

    @SuppressLint("CheckResult")
    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRssArticle3Binding,
        item: RssArticle,
        payloads: MutableList<Any>
    ) {
        binding.run {
            tvTitle.text = item.title
            tvPubDate.text = item.pubDate
            val imageUrl = item.image
            imageView.adjustViewBounds = true
            val layoutParams = imageView.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            val cachedSize = if (imageUrl != null) imageSizeCache[imageUrl] else null
            if (cachedSize == null) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                val parentWidth = imageView.width
                if (parentWidth <= 0) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    val (width, height) = cachedSize
                    val aspectRatio = height.toFloat() / width.toFloat()
                    val calculatedHeight = (parentWidth * aspectRatio).toInt()
                    layoutParams.height = calculatedHeight
                }
            }
            imageView.layoutParams = layoutParams
            val options = RequestOptions()
                .set(OkHttpModelLoader.sourceOriginOption, item.origin)
            val imageRequest = ImageLoader.load(context, imageUrl)
                .apply(options)
                .placeholder(R.drawable.transparent_placeholder) //svg图会依靠这个进行尺寸约束
            if (cachedSize == null) {
                imageRequest.addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        val width = resource.intrinsicWidth
                        val height = resource.intrinsicHeight
                        if (width > 0 && height > 0 && imageUrl != null) {
                            imageSizeCache.put(imageUrl, Pair(width, height))
                        }
                        return false
                    }
                })
            }
            imageRequest.into(imageView)
            if (item.read) {
                tvTitle.setTextColor(context.getCompatColor(R.color.tv_text_summary))
            } else {
                tvTitle.setTextColor(context.getCompatColor(R.color.primaryText))
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRssArticle3Binding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.readRss(it)
            }
        }
    }

}