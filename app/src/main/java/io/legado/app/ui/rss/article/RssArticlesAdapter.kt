package io.legado.app.ui.rss.article

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssArticle
import kotlinx.android.synthetic.main.item_rss_article.view.*


class RssArticlesAdapter(context: Context) :
    SimpleRecyclerAdapter<RssArticle>(context, R.layout.item_rss_article) {

    override fun convert(holder: ItemViewHolder, item: RssArticle, payloads: MutableList<Any>) {
        with(holder.itemView) {
            title.text = item.title
            pub_date.text = item.pubDate
            if (item.author.isNullOrBlank()) {
                author.text = item.link
            } else {
                author.text = item.author
            }
        }
    }


}