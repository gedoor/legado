package io.legado.app.ui.main.bookshelf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.help.ImageLoader
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.invisible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class BookshelfAdapter(private val callBack: CallBack) :
    PagedListAdapter<Book, BookshelfAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
                oldItem.bookUrl == newItem.bookUrl

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
                oldItem.durChapterTitle == newItem.durChapterTitle
                        && oldItem.latestChapterTitle == newItem.latestChapterTitle
                        && oldItem.durChapterTime == newItem.durChapterTime
                        && oldItem.lastCheckTime == newItem.lastCheckTime
        }
    }

    fun notification(bookUrl: String) {
        for (i in 0..itemCount) {
            getItem(i)?.let {
                if (it.bookUrl == bookUrl) {
                    notifyItemChanged(i)
                    return
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_bookshelf_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        currentList?.get(position)?.let {
            holder.bind(it, callBack)
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        init {
            itemView.setBackgroundColor(ThemeStore.backgroundColor(itemView.context))
        }

        fun bind(book: Book, callBack: CallBack) = with(itemView) {
            this.setBackgroundColor(context.getCompatColor(R.color.background))
            tv_name.text = book.name
            tv_author.text = book.author
            tv_read.text = book.durChapterTitle
            tv_last.text = book.latestChapterTitle
            book.getDisplayCover()?.let {
                ImageLoader.load(context, it)//Glide自动识别http://和file://
                    .placeholder(R.drawable.img_cover_default)
                    .error(R.drawable.img_cover_default)
                    .centerCrop()
                    .setAsDrawable(iv_cover)
            }
            itemView.onClick { callBack.open(book) }
            itemView.onLongClick {
                callBack.openBookInfo(book)
                true
            }
            if (book.origin != BookType.local && callBack.isUpdate(book.bookUrl)) {
                bv_unread.invisible()
                rl_loading.show()
            } else {
                rl_loading.hide()
                bv_unread.setBadgeCount(book.getUnreadChapterNum())
                bv_unread.setHighlight(book.lastCheckCount > 0)
            }
        }
    }

    interface CallBack {
        fun open(book: Book)
        fun openBookInfo(book: Book)
        fun isUpdate(bookUrl: String): Boolean
    }
}