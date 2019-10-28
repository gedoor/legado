package io.legado.app.ui.book.search

import android.content.Context
import android.view.View
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.ImageLoader
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.iv_cover
import kotlinx.android.synthetic.main.item_bookshelf_list.view.tv_name
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class SearchAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_search) {

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            bind(holder.itemView, item)
        } else {
            bindChange(holder.itemView, item, payloads)
        }
    }

    private fun bind(itemView: View, searchBook: SearchBook) {
        with(itemView) {
            tv_name.text = searchBook.name
            tv_author.text = context.getString(R.string.author_show, searchBook.author)
            bv_originCount.setBadgeCount(searchBook.origins?.size ?: 1)
            if (searchBook.latestChapterTitle.isNullOrEmpty()) {
                tv_lasted.gone()
            } else {
                tv_lasted.text = context.getString(R.string.lasted_show, searchBook.latestChapterTitle)
                tv_lasted.visible()
            }
            tv_introduce.text = context.getString(R.string.intro_show, searchBook.intro)
            val kinds = searchBook.getKindList()
            if (kinds.isEmpty()) {
                ll_kind.gone()
            } else {
                ll_kind.visible()
                for (index in 0..2) {
                    if (kinds.size > index) {
                        when (index) {
                            0 -> {
                                tv_kind.text = kinds[index]
                                tv_kind.visible()
                            }
                            1 -> {
                                tv_kind_1.text = kinds[index]
                                tv_kind_1.visible()
                            }
                            2 -> {
                                tv_kind_2.text = kinds[index]
                                tv_kind_2.visible()
                            }
                        }
                    } else {
                        when (index) {
                            0 -> tv_kind.gone()
                            1 -> tv_kind_1.gone()
                            2 -> tv_kind_2.gone()
                        }
                    }
                }
            }
            searchBook.coverUrl.let {
                ImageLoader.load(context, it)//Glide自动识别http://和file://
                    .placeholder(R.drawable.image_cover_default)
                    .error(R.drawable.image_cover_default)
                    .centerCrop()
                    .setAsDrawable(iv_cover)
            }
            onClick {
                callBack.showBookInfo(searchBook.name, searchBook.author)
            }
        }
    }

    private fun bindChange(itemView: View, searchBook: SearchBook, payloads: MutableList<Any>) {
        with(itemView) {
            when (payloads[0]) {
                1 -> bv_originCount.setBadgeCount(searchBook.origins?.size ?: 1)
                2 -> searchBook.coverUrl.let {
                    ImageLoader.load(context, it)//Glide自动识别http://和file://
                        .placeholder(R.drawable.image_cover_default)
                        .error(R.drawable.image_cover_default)
                        .centerCrop()
                        .setAsDrawable(iv_cover)
                }
                3 -> {
                    val kinds = searchBook.getKindList()
                    if (kinds.isEmpty()) {
                        ll_kind.gone()
                    } else {
                        ll_kind.visible()
                        for (index in 0..2) {
                            if (kinds.size > index) {
                                when (index) {
                                    0 -> {
                                        tv_kind.text = kinds[index]
                                        tv_kind.visible()
                                    }
                                    1 -> {
                                        tv_kind_1.text = kinds[index]
                                        tv_kind_1.visible()
                                    }
                                    2 -> {
                                        tv_kind_2.text = kinds[index]
                                        tv_kind_2.visible()
                                    }
                                }
                            } else {
                                when (index) {
                                    0 -> tv_kind.gone()
                                    1 -> tv_kind_1.gone()
                                    2 -> tv_kind_2.gone()
                                }
                            }
                        }
                    }
                }
                4 -> {
                    if (searchBook.latestChapterTitle.isNullOrEmpty()) {
                        tv_lasted.gone()
                    } else {
                        tv_lasted.text = context.getString(
                            R.string.lasted_show,
                            searchBook.latestChapterTitle
                        )
                        tv_lasted.visible()
                    }
                }
                5 -> tv_introduce.text =
                    context.getString(R.string.intro_show, searchBook.intro)
            }
        }
    }

    interface CallBack {
        fun showBookInfo(name: String, author: String)
    }
}