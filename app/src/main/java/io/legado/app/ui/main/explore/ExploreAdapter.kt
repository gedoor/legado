package io.legado.app.ui.main.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.widget.PopupMenu
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.ACache
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_find_book.view.*
import kotlinx.android.synthetic.main.item_text.view.*
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick


class ExploreAdapter(context: Context, private val scope: CoroutineScope, val callBack: CallBack) :
    SimpleRecyclerAdapter<BookSource>(context, R.layout.item_find_book) {

    var exIndex = 0

    override fun convert(holder: ItemViewHolder, item: BookSource, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                tv_name.text = item.bookSourceName
                ll_title.onClick {
                    val oldEx = exIndex
                    exIndex = if (exIndex == holder.layoutPosition) -1 else holder.layoutPosition
                    notifyItemChanged(oldEx, false)
                    if (exIndex != -1) {
                        notifyItemChanged(holder.layoutPosition, false)
                    }
                    callBack.scrollTo(holder.layoutPosition)
                }
                ll_title.onLongClick {
                    val popupMenu = PopupMenu(context, ll_title)
                    popupMenu.menu.add(Menu.NONE, R.id.menu_edit, Menu.NONE, R.string.edit)
                    popupMenu.menu.add(Menu.NONE, R.id.menu_top, Menu.NONE, R.string.to_top)
                    popupMenu.menu.add(Menu.NONE, R.id.menu_refresh, Menu.NONE, R.string.refresh)
                    popupMenu.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.menu_edit -> callBack.editSource(item.bookSourceUrl)
                            R.id.menu_top -> callBack.toTop(item)
                            R.id.menu_refresh -> {
                                ACache.get(context, "explore")?.remove(item.bookSourceUrl)
                                notifyItemChanged(holder.layoutPosition)
                            }
                        }
                        true
                    }
                    popupMenu.show()
                    true
                }
            }
            if (exIndex == holder.layoutPosition) {
                iv_status.setImageResource(R.drawable.ic_remove)
                rotate_loading.loadingColor = context.accentColor
                rotate_loading.show()
                Coroutine.async(scope) {
                    item.getExploreKinds()
                }.onSuccess { kindList ->
                    if (!kindList.isNullOrEmpty()) {
                        gl_child.visible()
                        gl_child.removeAllViews()
                        kindList.map { kind ->
                            val tv = LayoutInflater.from(context)
                                .inflate(R.layout.item_text, gl_child, false)
                            tv.text_view.text = kind.title
                            tv.text_view.onClick {
                                kind.url?.let { kindUrl ->
                                    callBack.openExplore(
                                        item.bookSourceUrl,
                                        kind.title,
                                        kindUrl
                                    )
                                }
                            }
                            gl_child.addView(tv)
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
        fun editSource(sourceUrl: String)
        fun toTop(source: BookSource)
    }
}