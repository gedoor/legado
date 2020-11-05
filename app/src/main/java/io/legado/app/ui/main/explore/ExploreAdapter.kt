package io.legado.app.ui.main.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.ACache
import io.legado.app.utils.dp
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_fillet_text.view.*
import kotlinx.android.synthetic.main.item_find_book.view.*
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick


class ExploreAdapter(context: Context, private val scope: CoroutineScope, val callBack: CallBack) :
    SimpleRecyclerAdapter<BookSource>(context, R.layout.item_find_book) {
    private var exIndex = -1
    private var scrollTo = -1

    override fun convert(holder: ItemViewHolder, item: BookSource, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (holder.layoutPosition == getActualItemCount() - 1) {
                setPadding(16.dp, 12.dp, 16.dp, 12.dp)
            } else {
                setPadding(16.dp, 12.dp, 16.dp, 0)
            }
            if (payloads.isEmpty()) {
                tv_name.text = item.bookSourceName
            }
            if (exIndex == holder.layoutPosition) {
                iv_status.setImageResource(R.drawable.ic_arrow_down)
                rotate_loading.loadingColor = context.accentColor
                rotate_loading.show()
                if (scrollTo >= 0) {
                    callBack.scrollTo(scrollTo)
                }
                Coroutine.async(scope) {
                    item.getExploreKinds()
                }.onSuccess { kindList ->
                    if (!kindList.isNullOrEmpty()) {
                        gl_child.visible()
                        gl_child.removeAllViews()
                        kindList.map { kind ->
                            val tv = LayoutInflater.from(context)
                                .inflate(R.layout.item_fillet_text, gl_child, false)
                            gl_child.addView(tv)
                            tv.text_view.text = kind.title
                            if (!kind.url.isNullOrEmpty()) {
                                tv.text_view.onClick {
                                    callBack.openExplore(
                                        item.bookSourceUrl,
                                        kind.title,
                                        kind.url.toString()
                                    )
                                }
                            }
                        }
                    }
                }.onFinally {
                    rotate_loading.hide()
                    if (scrollTo >= 0) {
                        callBack.scrollTo(scrollTo)
                        scrollTo = -1
                    }
                }
            } else {
                iv_status.setImageResource(R.drawable.ic_arrow_right)
                rotate_loading.hide()
                gl_child.gone()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            ll_title.onClick {
                val position = holder.layoutPosition
                val oldEx = exIndex
                exIndex = if (exIndex == position) -1 else position
                notifyItemChanged(oldEx, false)
                if (exIndex != -1) {
                    scrollTo = position
                    callBack.scrollTo(position)
                    notifyItemChanged(position, false)
                }
            }
            ll_title.onLongClick {
                showMenu(ll_title, holder.layoutPosition)
            }
        }
    }

    fun compressExplore(): Boolean {
        return if (exIndex < 0) {
            false
        } else {
            val oldExIndex = exIndex
            exIndex = -1
            notifyItemChanged(oldExIndex)
            true
        }
    }

    private fun showMenu(view: View, position: Int): Boolean {
        val source = getItem(position) ?: return true
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.explore_item)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_edit -> callBack.editSource(source.bookSourceUrl)
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_refresh -> {
                    ACache.get(context, "explore").remove(source.bookSourceUrl)
                    notifyItemChanged(position)
                }
                R.id.menu_del -> Coroutine.async(scope) {
                    App.db.bookSourceDao().delete(source)
                }
            }
            true
        }
        popupMenu.show()
        return true
    }

    interface CallBack {
        fun scrollTo(pos: Int)
        fun openExplore(sourceUrl: String, title: String, exploreUrl: String)
        fun editSource(sourceUrl: String)
        fun toTop(source: BookSource)
    }
}