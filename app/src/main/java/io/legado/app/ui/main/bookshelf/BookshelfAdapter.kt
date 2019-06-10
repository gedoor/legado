package io.legado.app.ui.main.bookshelf

import android.text.TextUtils.isEmpty
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.lib.theme.ThemeStore
import kotlinx.android.synthetic.main.item_bookshelf_list.view.*
import kotlinx.android.synthetic.main.item_relace_rule.view.tv_name
import java.io.File

class BookshelfAdapter : PagedListAdapter<Book, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
                    oldItem.descUrl == newItem.descUrl

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
                    oldItem.descUrl == newItem.descUrl
                            && oldItem.durChapterTitle == newItem.durChapterTitle
                            && oldItem.latestChapterTitle == newItem.latestChapterTitle
        }
    }

    var callBack: CallBack? = null

    override fun getItemViewType(position: Int): Int {
        if (position == itemCount - 1) {
            return 1
        }
        return super.getItemViewType(position)
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            return AddViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookshelf_list_add, parent, false))
        }
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bookshelf_list, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyViewHolder -> {
                currentList?.get(position)?.let {
                    holder.bind(it, callBack)
                }
            }
            is AddViewHolder -> holder.bind(callBack)
        }

    }

    class AddViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(callBack: CallBack?) = with(itemView) {
            setOnClickListener { callBack?.search() }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        init {
            itemView.setBackgroundColor(ThemeStore.backgroundColor(itemView.context))
        }

        fun bind(book: Book, callBack: CallBack?) = with(itemView) {
            tv_name.text = book.name
            tv_author.text = book.author
            tv_read.text = book.durChapterTitle
            tv_last.text = book.latestChapterTitle
            val cover = if (isEmpty(book.customCoverUrl)) book.coverUrl else book.customCoverUrl
            cover?.let {
                if (it.startsWith("http")) {
                    Glide.with(itemView).load(it)
                            .placeholder(R.drawable.img_cover_default)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .into(iv_cover)
                } else {
                    Glide.with(itemView).load(File(it))
                            .placeholder(R.drawable.img_cover_default)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .into(iv_cover)
                }
            }
            itemView.setOnClickListener { callBack?.open(book) }
        }
    }

    interface CallBack {
        fun open(book: Book)
        fun search()
    }
}