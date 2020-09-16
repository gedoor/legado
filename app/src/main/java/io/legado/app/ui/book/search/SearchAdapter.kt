package io.legado.app.ui.book.search

import android.content.Context
import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.iv_cover
import kotlinx.android.synthetic.main.item_bookshelf_list.view.tv_name
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class SearchAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_search) {

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            bind(holder.itemView, item)
        } else {
            bindChange(holder.itemView, item, bundle)
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.showBookInfo(it.name, it.author)
                }
            }
        }
    }

    private fun bind(itemView: View, searchBook: SearchBook) {
        with(itemView) {
            tv_name.text = searchBook.name
            tv_author.text = context.getString(R.string.author_show, searchBook.author)
            bv_originCount.setBadgeCount(searchBook.origins.size)
            upLasted(itemView, searchBook.latestChapterTitle)
            if (searchBook.intro.isNullOrEmpty()) {
                tv_introduce.text =
                    context.getString(R.string.intro_show_null)
            } else {
                tv_introduce.text =
                    context.getString(R.string.intro_show, searchBook.intro)
            }
            upKind(itemView, searchBook.getKindList())
            iv_cover.load(searchBook.coverUrl, searchBook.name, searchBook.author)

        }
    }

    private fun bindChange(itemView: View, searchBook: SearchBook, bundle: Bundle) {
        with(itemView) {
            bundle.keySet().map {
                when (it) {
                    "name" -> tv_name.text = searchBook.name
                    "author" -> tv_author.text =
                        context.getString(R.string.author_show, searchBook.author)
                    "origins" -> bv_originCount.setBadgeCount(searchBook.origins.size)
                    "last" -> upLasted(itemView, searchBook.latestChapterTitle)
                    "intro" -> {
                        if (searchBook.intro.isNullOrEmpty()) {
                            tv_introduce.text =
                                context.getString(R.string.intro_show_null)
                        } else {
                            tv_introduce.text =
                                context.getString(R.string.intro_show, searchBook.intro)
                        }
                    }
                    "kind" -> upKind(itemView, searchBook.getKindList())
                    "cover" -> iv_cover.load(
                        searchBook.coverUrl,
                        searchBook.name,
                        searchBook.author
                    )
                }
            }
        }
    }

    private fun upLasted(itemView: View, latestChapterTitle: String?) {
        with(itemView) {
            if (latestChapterTitle.isNullOrEmpty()) {
                tv_lasted.gone()
            } else {
                tv_lasted.text =
                    context.getString(
                        R.string.lasted_show,
                        latestChapterTitle
                    )
                tv_lasted.visible()
            }
        }
    }

    private fun upKind(itemView: View, kinds: List<String>) = with(itemView) {
        if (kinds.isEmpty()) {
            ll_kind.gone()
        } else {
            ll_kind.visible()
            ll_kind.setLabels(kinds)
        }
    }

    interface CallBack {
        fun showBookInfo(name: String, author: String)
    }
}