package io.legado.app.ui.rss.article

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.entities.RssArticle
import io.legado.app.databinding.ItemRssArticle1Binding
import io.legado.app.help.ImageLoader
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.textColorResource

class RssArticlesAdapter1(context: Context, callBack: CallBack) :
    BaseRssArticlesAdapter<ItemRssArticle1Binding>(context, callBack) {

    override fun getViewBinding(parent: ViewGroup): ItemRssArticle1Binding {
        return ItemRssArticle1Binding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRssArticle1Binding,
        item: RssArticle,
        payloads: MutableList<Any>
    ) {
        with(binding) {
            tvTitle.text = item.title
            tvPubDate.text = item.pubDate
            if (item.image.isNullOrBlank() && !callBack.isGridLayout) {
                imageView.gone()
            } else {
                ImageLoader.load(context, item.image).apply {
                    if (callBack.isGridLayout) {
                        placeholder(R.drawable.image_rss_article)
                    } else {
                        addListener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                imageView.gone()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                imageView.visible()
                                return false
                            }

                        })
                    }
                }.into(imageView)
            }
            if (item.read) {
                tvTitle.textColorResource = R.color.tv_text_summary
            } else {
                tvTitle.textColorResource = R.color.primaryText
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRssArticle1Binding) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callBack.readRss(it)
            }
        }
    }

}