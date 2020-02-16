package io.legado.app.ui.rss.favorites

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssStar
import io.legado.app.help.ImageLoader
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_rss_article.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class RssFavoritesAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssStar>(context, R.layout.item_rss_article) {

    override fun convert(holder: ItemViewHolder, item: RssStar, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_title.text = item.title
            tv_pub_date.text = item.pubDate
            if (item.image.isNullOrBlank()) {
                image_view.gone()
            } else {
                ImageLoader.load(context, item.image)
                    .addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            image_view.gone()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            image_view.visible()
                            return false
                        }

                    })
                    .into(image_view)
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callBack.readRss(it)
            }
        }
    }

    interface CallBack {
        fun readRss(rssStar: RssStar)
    }
}