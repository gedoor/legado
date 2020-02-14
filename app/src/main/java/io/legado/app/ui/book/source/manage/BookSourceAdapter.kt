package io.legado.app.ui.book.source.manage

import android.content.Context
import android.os.Bundle
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ItemTouchCallback.OnItemTouchCallbackListener
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_book_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*

class BookSourceAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<BookSource>(context, R.layout.item_book_source),
    OnItemTouchCallbackListener {

    private val selected = linkedSetOf<BookSource>()

    fun selectAll() {
        getItems().forEach {
            selected.add(it)
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().forEach {
            if (selected.contains(it)) {
                selected.remove(it)
            } else {
                selected.add(it)
            }
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun getSelection(): LinkedHashSet<BookSource> {
        val selection = linkedSetOf<BookSource>()
        getItems().map {
            if (selected.contains(it)) {
                selection.add(it)
            }
        }
        return selection
    }

    override fun convert(holder: ItemViewHolder, item: BookSource, payloads: MutableList<Any>) {
        with(holder.itemView) {
            val payload = payloads.getOrNull(0) as? Bundle
            if (payload == null) {
                this.setBackgroundColor(context.backgroundColor)
                if (item.bookSourceGroup.isNullOrEmpty()) {
                    cb_book_source.text = item.bookSourceName
                } else {
                    cb_book_source.text =
                        String.format("%s (%s)", item.bookSourceName, item.bookSourceGroup)
                }
                swt_enabled.isChecked = item.enabled
                swt_enabled.onClick {
                    item.enabled = swt_enabled.isChecked
                    callBack.update(item)
                }
                cb_book_source.isChecked = selected.contains(item)
                cb_book_source.setOnClickListener {
                    if (cb_book_source.isChecked) {
                        selected.add(item)
                    } else {
                        selected.remove(item)
                    }
                    callBack.upCountView()
                }
                iv_edit.onClick { callBack.edit(item) }
                iv_menu_more.onClick {
                    val popupMenu = PopupMenu(context, it)
                    popupMenu.inflate(R.menu.book_source_item)
                    val qyMenu = popupMenu.menu.findItem(R.id.menu_enable_explore)
                    if (item.exploreUrl.isNullOrEmpty()) {
                        qyMenu.isVisible = false
                    } else {
                        if (item.enabledExplore) {
                            qyMenu.setTitle(R.string.disable_explore)
                        } else {
                            qyMenu.setTitle(R.string.enable_explore)
                        }
                    }
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_top -> callBack.toTop(item)
                            R.id.menu_del -> callBack.del(item)
                            R.id.menu_enable_explore -> {
                                item.enabledExplore = !item.enabledExplore
                                callBack.update(item)
                                iv_explore.visible(item.showExplore())
                            }
                        }
                        true
                    }
                    popupMenu.show()
                }
                iv_explore.visible(item.showExplore())
            } else {
                payload.keySet().map {
                    when (it) {
                        "selected" -> cb_book_source.isChecked = selected.contains(item)
                        "name", "group" -> if (item.bookSourceGroup.isNullOrEmpty()) {
                            cb_book_source.text = item.bookSourceName
                        } else {
                            cb_book_source.text =
                                String.format("%s (%s)", item.bookSourceName, item.bookSourceGroup)
                        }
                        "enabled" -> swt_enabled.isChecked = payload.getBoolean(it)
                        "showExplore" -> iv_explore.visible(payload.getBoolean(it))
                    }
                }
            }
        }
    }

    override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.customOrder == targetItem.customOrder) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.customOrder
                srcItem.customOrder = targetItem.customOrder
                targetItem.customOrder = srcOrder
                movedItems.add(srcItem)
                movedItems.add(targetItem)
            }
        }
        Collections.swap(getItems(), srcPosition, targetPosition)
        notifyItemMoved(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<BookSource>()

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    interface CallBack {
        fun del(bookSource: BookSource)
        fun edit(bookSource: BookSource)
        fun update(vararg bookSource: BookSource)
        fun toTop(bookSource: BookSource)
        fun upOrder()
        fun upCountView()
    }
}