package io.legado.app.ui.rss.article

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
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
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import androidx.core.content.edit
import splitties.init.appCtx

class RssArticlesAdapter3(context: Context, callBack: CallBack) :
    BaseRssArticlesAdapter<ItemRssArticle3Binding>(context, callBack) {

    companion object {
        private val imageHigh = LruCache<String, Int>(9999)
        private const val PREF_NAME = "rss_image_heights"
        private val prefs: SharedPreferences by lazy {
            appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
        private fun getImageHeight(url: String): Int {
            var height = imageHigh[url] ?: 0
            if (height == 0) {
                if (prefs.contains(url)) {
                    height = prefs.getInt(url, 0)
                    if (height > 0) {
                        imageHigh.put(url, height)
                    }
                }
            }
            return height
        }
        private fun putImageHeight(url: String, height: Int) {
            if (height <= 0) return
            imageHigh.put(url, height)
            prefs.edit { putInt(url, height) }
        }
        fun clearImageHeight() {
            imageHigh.evictAll()
            prefs.edit { clear() }
        }
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
        if (payloads.isNotEmpty()) {
            payloads.forEach { payload ->
                when (payload) {
                    "read" -> {
                        if (item.read) {
                            binding.tvTitle.setTextColor(context.getCompatColor(R.color.tv_text_summary))
                        } else {
                            binding.tvTitle.setTextColor(context.getCompatColor(R.color.primaryText))
                        }
                    }
                    "title" -> {
                        binding.tvTitle.text = item.title
                    }
                }
            }
            return
        }
        binding.run {
            tvTitle.text = item.title
            if (item.read) {
                tvTitle.setTextColor(context.getCompatColor(R.color.tv_text_summary))
            } else {
                tvTitle.setTextColor(context.getCompatColor(R.color.primaryText))
            }
            tvPubDate.text = item.pubDate
            val imageUrl = item.image
            val options = RequestOptions()
                .set(OkHttpModelLoader.sourceOriginOption, item.origin)
            val imageRequest = ImageLoader.load(context, imageUrl)
                .apply(options)
                .placeholder(R.drawable.transparent_placeholder) //svg图会依靠这个进行尺寸约束
            if (imageUrl.isNullOrEmpty()) {
                return
            }
            val layoutParams = imageView.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = getImageHeight(imageUrl)
            if (height == 0) {
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
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
                        if (width > 0 && height > 0) {
                            val aspectRatio = height.toFloat() / width.toFloat()
                            if (target is ImageViewTarget) {
                                putImageHeight(imageUrl, (target.view.width * aspectRatio).toInt())
                            }
                        }
                        return false
                    }
                })
            } else {
                layoutParams.height = height
            }
            imageView.layoutParams = layoutParams
            imageView.adjustViewBounds = true //自动调整ImageView的边界来适应图片的宽高比
            imageRequest.into(imageView)
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