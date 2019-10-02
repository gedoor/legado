package io.legado.app.ui.rss.article

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssArticle
import io.legado.app.help.ImageLoader
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_rss_article.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class RssArticlesAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssArticle>(context, R.layout.item_rss_article) {

    override fun convert(holder: ItemViewHolder, item: RssArticle, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_title.text = item.title
            tv_pub_date.text = item.pubDate
            onClick {
                callBack.readRss(item)
            }
            if (item.image.isNullOrBlank()) {
                image_view.gone()
            } else {
                image_view.visible()
                ImageLoader.load(context, item.image)
                    .setAsBitmap(image_view)
            }
        }
    }

    interface CallBack {
        fun readRss(rssArticle: RssArticle)
    }
}