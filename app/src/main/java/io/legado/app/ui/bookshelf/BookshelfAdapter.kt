package io.legado.app.ui.bookshelf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.help.ImageLoader
import io.legado.app.lib.theme.ATH
import kotlinx.android.synthetic.main.item_bookshelf_list.view.*
import kotlinx.android.synthetic.main.item_relace_rule.view.tv_name

class BookshelfAdapter : PagedListAdapter<Book, BookshelfAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
                oldItem.bookUrl == newItem.bookUrl

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
                oldItem.bookUrl == newItem.bookUrl
                        && oldItem.durChapterTitle == newItem.durChapterTitle
                        && oldItem.latestChapterTitle == newItem.latestChapterTitle
                        && oldItem.durChapterTime == newItem.durChapterTime
                        && oldItem.lastCheckTime == newItem.lastCheckTime
        }
    }

    var callBack: CallBack? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_bookshelf_list,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        currentList?.get(position)?.let {
            holder.bind(it, callBack)
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        init {
            ATH.applyBackgroundTint(itemView)
        }

        fun bind(book: Book, callBack: CallBack?) = with(itemView) {
            tv_name.text = book.name
            tv_author.text = book.author
            tv_read.text = book.durChapterTitle
            tv_last.text = book.latestChapterTitle
            book.getDisplayCover()?.let {
                ImageLoader.load(context, it)//Glide自动识别http://和file:///
                    .placeholder(R.drawable.img_cover_default)
                    .error(R.drawable.img_cover_default)
                    .centerCrop()
                    .setAsDrawable(iv_cover)
            }
            itemView.setOnClickListener { callBack?.open(book) }
        }
    }

    interface CallBack {
        fun open(book: Book)
        fun search()
    }
}