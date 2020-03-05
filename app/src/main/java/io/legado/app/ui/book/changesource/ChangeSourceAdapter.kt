package io.legado.app.ui.book.changesource

import android.content.Context
import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_change_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ChangeSourceAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_change_source) {

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        val bundle = payloads.getOrNull(0) as? Bundle
        holder.itemView.apply {
            if (bundle == null) {
                tv_origin.text = item.originName
                tv_last.text = item.getDisplayLastChapterTitle()
                if (callBack.bookUrl == item.bookUrl) {
                    iv_checked.visible()
                } else {
                    iv_checked.invisible()
                }
            } else {
                bundle.keySet().map {
                    when (it) {
                        "name" -> tv_origin.text = item.originName
                        "latest" -> tv_last.text = item.getDisplayLastChapterTitle()
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callBack.changeTo(it)
            }
        }
    }

    interface CallBack {
        val bookUrl: String?
        fun changeTo(searchBook: SearchBook)
    }
}