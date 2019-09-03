package io.legado.app.ui.main.findbook

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.utils.gone
import io.legado.app.utils.invisible
import kotlinx.android.synthetic.main.item_find_book.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class FindBookAdapter:PagedListAdapter<BookSource, FindBookAdapter.MyViewHolder>(DIFF_CALLBACK) {

    var exIndex = 0

    companion object {
        var DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookSource>() {
            override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.bookSourceUrl == newItem.bookSourceUrl

            override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.bookSourceUrl == newItem.bookSourceUrl
                        && oldItem.bookSourceName == newItem.bookSourceName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_find_book, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int): Unit =
        with(holder.itemView) {
            currentList?.get(position)?.let { bookSource ->
                val bgShape: GradientDrawable? = tv_name.background as? GradientDrawable
                bgShape?.setStroke(2, ColorUtils.getRandomColor())
                tv_name.text = bookSource.bookSourceName
                ll_title.onClick {
                    val oldEx = exIndex
                    exIndex = position
                    notifyItemChanged(oldEx)
                    notifyItemChanged(position)
                }
                if (exIndex == position) {
                    gl_child.invisible()
                    bookSource.getExploreRule().getExploreKinds(bookSource.bookSourceUrl)?.let {
                        it.map { kind ->
                            val tv = TextView(context)
                            tv.text = kind.title
                            tv.onClick { }
                            gl_child.addView(tv)
                        }
                    }
                } else {
                    gl_child.gone()
                }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}