package io.legado.app.ui.rss.article

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssArticle
import io.legado.app.help.ImageLoader
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_rss_article.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.textColorResource


class RssArticlesAdapter(context: Context, layoutId: Int, private val isGridLayout: Boolean, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssArticle>(context, layoutId) {

    fun emptyImage(image_view: ImageView) {
        if (isGridLayout)
            image_view.setImageResource(R.drawable.rss_img_default)
        else
            image_view.gone()
    }

    override fun convert(holder: ItemViewHolder, item: RssArticle, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_title.text = item.title
            tv_pub_date.text = item.pubDate
            if (item.image.isNullOrBlank()) {
                emptyImage(image_view)
            } else {
                ImageLoader.load(context, item.image)
                    .addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            emptyImage(image_view)
                            return true
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
            if (item.read) {
                tv_title.textColorResource = R.color.tv_text_summary
            } else {
                tv_title.textColorResource = R.color.tv_text_default
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
        fun readRss(rssArticle: RssArticle)
    }
}