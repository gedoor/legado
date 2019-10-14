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
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.LogUtils
import io.legado.app.utils.invisible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class BooksAdapter(private val callBack: CallBack) :
    PagedListAdapter<Book, BooksAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean =
                oldItem.bookUrl == newItem.bookUrl

            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
                oldItem.durChapterTitle == newItem.durChapterTitle
                        && oldItem.name == newItem.name
                        && oldItem.getDisplayCover() == newItem.getDisplayCover()
                        && oldItem.latestChapterTitle == newItem.latestChapterTitle
                        && oldItem.durChapterTitle == newItem.durChapterTitle
        }
    }

    fun notification(bookUrl: String) {
        for (i in 0 until itemCount) {
            getItem(i)?.let {
                if (it.bookUrl == bookUrl) {
                    notifyItemChanged(i, 5)
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

    override fun onBindViewHolder(holder: MyViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            currentList?.get(position)?.let {
                holder.bind(it, callBack, payloads[0])
            }
        }
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

        fun bind(book: Book, callBack: CallBack) = with(itemView) {
            tv_name.text = book.name
            tv_author.text = book.author
            tv_read.text = book.durChapterTitle
            tv_last.text = book.latestChapterTitle
            book.getDisplayCover()?.let {
                ImageLoader.load(context, it)//Glide自动识别http://和file://
                    .placeholder(R.drawable.image_cover_default)
                    .error(R.drawable.image_cover_default)
                    .centerCrop()
                    .setAsDrawable(iv_cover)
            }
            itemView.onClick { callBack.open(book) }
            itemView.onLongClick {
                callBack.openBookInfo(book)
                true
            }
            if (book.origin != BookType.local && callBack.isUpdate(book.bookUrl)) {
                LogUtils.d(book.name, "loading")
                bv_unread.invisible()
                rl_loading.show()
            } else {
                LogUtils.d(book.name, "loadingHide")
                rl_loading.hide()
                bv_unread.setBadgeCount(book.getUnreadChapterNum())
                bv_unread.setHighlight(book.lastCheckCount > 0)
            }
        }

        fun bind(book: Book, callBack: CallBack, payload: Any) = with(itemView) {
            when (payload) {
                5 -> {
                    if (book.origin != BookType.local && callBack.isUpdate(book.bookUrl)) {
                        LogUtils.d(book.name, "loading")
                        bv_unread.invisible()
                        rl_loading.show()
                    } else {
                        LogUtils.d(book.name, "loadingHide")
                        rl_loading.hide()
                        bv_unread.setBadgeCount(book.getUnreadChapterNum())
                        bv_unread.setHighlight(book.lastCheckCount > 0)
                    }
                }
            }
        }
    }

    interface CallBack {
        fun open(book: Book)
        fun openBookInfo(book: Book)
        fun isUpdate(bookUrl: String): Boolean
    }
}