package io.legado.app.ui.chapterlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import kotlinx.android.synthetic.main.item_bookmark.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListAdapter(val callback: Callback) :
    PagedListAdapter<BookChapter, ChapterListAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookChapter>() {
            override fun areItemsTheSame(oldItem: BookChapter, newItem: BookChapter): Boolean =
                oldItem.bookUrl == newItem.bookUrl
                        && oldItem.url == newItem.url

            override fun areContentsTheSame(oldItem: BookChapter, newItem: BookChapter): Boolean =
                oldItem.title == newItem.title
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chapter_list, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, callback)
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(bookChapter: BookChapter, callback: Callback) = with(itemView) {
            if (callback.durChapterIndex() == bookChapter.index) {
                tv_chapter_name.setTextColor(context.accentColor)
            } else {
                tv_chapter_name.setTextColor(context.getCompatColor(R.color.tv_text_default))
            }
            tv_chapter_name.text = bookChapter.title
            itemView.onClick {
                callback.openChapter(bookChapter)
            }
            callback.book()?.let {
                tv_chapter_name.paint.isFakeBoldText = BookHelp.hasContent(it, bookChapter)
            }
        }
    }

    interface Callback {
        fun book(): Book?
        fun openChapter(bookChapter: BookChapter)
        fun durChapterIndex(): Int
    }
}