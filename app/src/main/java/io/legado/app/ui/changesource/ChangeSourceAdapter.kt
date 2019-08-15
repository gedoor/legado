package io.legado.app.ui.changesource

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_change_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ChangeSourceAdapter(context: Context) : SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_change_source) {

    var callBack: CallBack? = null

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        holder.itemView.apply {
            this.onClick {
                callBack?.changeTo(item)
            }
            tv_origin.text = item.originName
            tv_last.text = item.latestChapterTitle
            callBack?.let {
                if (it.curBookUrl() == item.bookUrl) {
                    iv_checked.visible()
                } else {
                    iv_checked.invisible()
                }
            } ?: iv_checked.invisible()
        }
    }

    interface CallBack {
        fun changeTo(searchBook: SearchBook)
        fun curBookUrl(): String
    }
}