package io.legado.app.ui.rss.article

import android.content.Context
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssArticle
import org.jetbrains.anko.sdk27.listeners.onClick


abstract class RssArticlesBaseAdapter(context: Context, private val layoutId: Int, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssArticle>(context, layoutId) {

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