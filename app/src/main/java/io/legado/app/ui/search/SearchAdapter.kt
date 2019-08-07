package io.legado.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.SearchShow
import io.legado.app.help.ImageLoader
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.iv_cover
import kotlinx.android.synthetic.main.item_bookshelf_list.view.tv_name
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class SearchAdapter : PagedListAdapter<SearchShow, SearchAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchShow>() {
            override fun areItemsTheSame(oldItem: SearchShow, newItem: SearchShow): Boolean =
                oldItem.name == newItem.name
                        && oldItem.author == newItem.author

            override fun areContentsTheSame(oldItem: SearchShow, newItem: SearchShow): Boolean =
                oldItem.name == newItem.name
                        && oldItem.author == newItem.author
        }
    }

    var callBack: CallBack? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, callBack)
        }
    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(searchBook: SearchShow, callBack: CallBack?) = with(itemView) {
            tv_name.text = searchBook.name
            bv_originCount.setBadgeCount(searchBook.originCount)
            tv_author.text = searchBook.author
            if (searchBook.latestChapterTitle.isNullOrEmpty()) {
                tv_lasted.gone()
            } else {
                tv_lasted.text = context.getString(R.string.book_search_last, searchBook.latestChapterTitle)
                tv_lasted.visible()
            }
            tv_introduce.text = searchBook.intro
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
                    .placeholder(R.drawable.img_cover_default)
                    .error(R.drawable.img_cover_default)
                    .centerCrop()
                    .setAsDrawable(iv_cover)
            }
            onClick {
                callBack?.showBookInfo(searchBook.name, searchBook.author)
            }
        }
    }

    interface CallBack {
        fun showBookInfo(name: String?, author: String?)
    }
}