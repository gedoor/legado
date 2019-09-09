package io.legado.app.ui.main.rss

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssSource

class RssAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssSource>(context, R.layout.item_rss) {

    override fun convert(holder: ItemViewHolder, item: RssSource, payloads: MutableList<Any>) {

    }

    interface CallBack {
        fun openRss()
    }
}