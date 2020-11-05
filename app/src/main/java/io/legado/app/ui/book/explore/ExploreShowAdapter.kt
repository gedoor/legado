package io.legado.app.ui.book.explore

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.iv_cover
import kotlinx.android.synthetic.main.item_bookshelf_list.view.tv_name
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ExploreShowAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_search) {

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        holder.itemView.apply {
            tv_name.text = item.name
            tv_author.text = context.getString(R.string.author_show, item.author)
            if (item.latestChapterTitle.isNullOrEmpty()) {
                tv_lasted.gone()
            } else {
                tv_lasted.text = context.getString(R.string.lasted_show, item.latestChapterTitle)
                tv_lasted.visible()
            }
            if (item.intro.isNullOrEmpty()) {
                tv_introduce.text = context.getString(R.string.intro_show_null)
            } else {
                tv_introduce.text = context.getString(R.string.intro_show, item.intro)
            }
            val kinds = item.getKindList()
            if (kinds.isEmpty()) {
                ll_kind.gone()
            } else {
                ll_kind.visible()
                ll_kind.setLabels(kinds)
            }
            iv_cover.load(item.coverUrl, item.name, item.author)
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callBack.showBookInfo(it.toBook())
            }
        }
    }

    interface CallBack {
        fun showBookInfo(book: Book)
    }
}