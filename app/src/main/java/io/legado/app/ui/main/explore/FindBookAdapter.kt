package io.legado.app.ui.main.explore

import android.content.Context
import android.view.LayoutInflater
import android.widget.GridLayout
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_find_book.view.*
import kotlinx.android.synthetic.main.item_text.view.*
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.anko.sdk27.listeners.onClick


class FindBookAdapter(context: Context, private val scope: CoroutineScope, val callBack: CallBack) :
    SimpleRecyclerAdapter<BookSource>(context, R.layout.item_find_book) {

    var exIndex = 0

    override fun convert(holder: ItemViewHolder, item: BookSource, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                tv_name.text = item.bookSourceName
                ll_title.onClick {
                    val oldEx = exIndex
                    if (exIndex == holder.layoutPosition) {
                        exIndex = -1
                    } else {
                        exIndex = holder.layoutPosition
                        notifyItemChanged(holder.layoutPosition, false)
                    }
                    notifyItemChanged(oldEx, false)
                    callBack.scrollTo(holder.layoutPosition)
                }
            }
            if (exIndex == holder.layoutPosition) {
                iv_status.setImageResource(R.drawable.ic_remove)
                rotate_loading.loadingColor = context.accentColor
                rotate_loading.show()
                Coroutine.async(scope) {
                    item.getExploreRule().getExploreKinds(item.bookSourceUrl)
                }.onSuccess {
                    it?.let {
                        gl_child.visible()
                        var rowNum = 0
                        var columnNum = 0
                        gl_child.removeAllViews()
                        it.map { kind ->
                            val tv = LayoutInflater.from(context)
                                .inflate(R.layout.item_text, gl_child, false)
                            tv.text_view.text = kind.title
                            tv.text_view.onClick {
                                callBack.openExplore(
                                    item.bookSourceUrl,
                                    kind.title,
                                    kind.url
                                )
                            }
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
                }.onFinally {
                    rotate_loading.hide()
                }
            } else {
                iv_status.setImageResource(R.drawable.ic_add)
                rotate_loading.hide()
                gl_child.gone()
            }
        }
    }

    interface CallBack {
        fun scrollTo(pos: Int)
        fun openExplore(sourceUrl: String, title: String, exploreUrl: String)
    }
}