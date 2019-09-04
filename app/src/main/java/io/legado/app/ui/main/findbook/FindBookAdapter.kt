package io.legado.app.ui.main.findbook

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_find_book.view.*
import kotlinx.android.synthetic.main.item_text.view.*
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
        return MyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_find_book, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int): Unit =
        with(holder.itemView) {
            currentList?.get(position)?.let { bookSource ->
                val bgShape: GradientDrawable? = tv_name.background as? GradientDrawable
                bgShape?.setStroke(2, ColorUtils.getRandomColor())
                tv_name.text = bookSource.bookSourceName
                ll_title.onClick {
                    val oldEx = exIndex
                    if (exIndex == position) {
                        exIndex = -1
                    } else {
                        exIndex = position
                        notifyItemChanged(position)
                    }
                    notifyItemChanged(oldEx)
                }
                if (exIndex == position) {
                    gl_child.visible()
                    bookSource.getExploreRule().getExploreKinds(bookSource.bookSourceUrl)?.let {
                        var rowNum = 0
                        var columnNum = 0
                        gl_child.removeAllViews()
                        it.map { kind ->
                            val tv = LayoutInflater.from(context)
                                .inflate(R.layout.item_text, gl_child, false)
                            tv.text_view.text = kind.title
                            val rowSpecs = GridLayout.spec(rowNum, 1.0f)
                            val colSpecs = GridLayout.spec(columnNum, 1.0f)
                            val params = GridLayout.LayoutParams(rowSpecs, colSpecs)
                            gl_child.addView(tv, params)
                            if (columnNum < 2) {
                                columnNum++
                            } else {
                                columnNum = 0
                                rowNum++
                            }
                        }
                    }
                } else {
                    gl_child.gone()
                }
        }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}