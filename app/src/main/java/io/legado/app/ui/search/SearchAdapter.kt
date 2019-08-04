package io.legado.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.ImageLoader
import kotlinx.android.synthetic.main.item_bookshelf_list.view.iv_cover
import kotlinx.android.synthetic.main.item_bookshelf_list.view.tv_name
import kotlinx.android.synthetic.main.item_search.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class SearchAdapter : PagedListAdapter<SearchBook, SearchAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchBook>() {
            override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean =
                oldItem.name == newItem.name
                        && oldItem.author == newItem.author

            override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean =
                oldItem.name == newItem.name
                        && oldItem.author == newItem.author
                        && oldItem.originCount == newItem.originCount
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

        fun bind(searchBook: SearchBook, callBack: CallBack?) = with(itemView) {
            tv_name.text = String.format("%s(%s)", searchBook.name, searchBook.author)
            tv_lasted.text = context.getString(R.string.book_search_last, searchBook.latestChapterTitle)
            tv_introduce.text = searchBook.intro
            searchBook.coverUrl.let {
                ImageLoader.load(context, it)//Glide自动识别http://和file://
                    .placeholder(R.drawable.img_cover_default)
                    .error(R.drawable.img_cover_default)
                    .centerCrop()
                    .setAsDrawable(iv_cover)
            }
            onClick {
                callBack?.showBookInfo(searchBook)
            }
        }
    }

    interface CallBack {
        fun showBookInfo(searchBook: SearchBook)
    }
}