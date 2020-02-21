package io.legado.app.ui.main.rss

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssSource
import io.legado.app.help.ImageLoader
import kotlinx.android.synthetic.main.item_rss.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class RssAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssSource>(context, R.layout.item_rss) {

    override fun convert(holder: ItemViewHolder, item: RssSource, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_name.text = item.sourceName
            ImageLoader.load(context, item.sourceIcon)
                .centerCrop()
                .placeholder(R.drawable.image_rss)
                .error(R.drawable.image_rss)
                .into(iv_icon)
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callBack.openRss(it)
            }
        }
    }

    interface CallBack {
        fun openRss(rssSource: RssSource)
    }
}