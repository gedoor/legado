package io.legado.app.ui.changecover

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.ImageLoader
import kotlinx.android.synthetic.main.item_cover.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class CoverAdapter(context: Context) :
    SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_cover) {

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        with(holder.itemView) {
            item.coverUrl?.let {
                ImageLoader.load(context, it)
                    .centerCrop()
                    .into(iv_cover)
            }
            tv_source.text = item.originName
            onClick {

            }
        }
    }

    interface CallBack {
        fun changeTo()
    }
}