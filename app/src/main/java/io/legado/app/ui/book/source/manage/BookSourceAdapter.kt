package io.legado.app.ui.book.source.manage

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ItemTouchCallback.OnItemTouchCallbackListener
import io.legado.app.lib.theme.backgroundColor
import kotlinx.android.synthetic.main.item_book_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class BookSourceAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<BookSource>(context, R.layout.item_book_source),
    OnItemTouchCallbackListener {

    private val selected = linkedSetOf<BookSource>()

    fun selectAll() {
        getItems().forEach {
            selected.add(it)
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
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

    override fun onSwiped(adapterPosition: Int) {

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
                callBack.update(srcItem, targetItem)
            }
        }
        return true
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
                }
                iv_edit.onClick { callBack.edit(item) }
                iv_menu_more.onClick {
                    val popupMenu = PopupMenu(context, it)
                    popupMenu.menu.add(Menu.NONE, R.id.menu_top, Menu.NONE, R.string.to_top)
                    popupMenu.menu.add(Menu.NONE, R.id.menu_del, Menu.NONE, R.string.delete)
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_top -> callBack.toTop(item)
                            R.id.menu_del -> callBack.del(item)
                        }
                        true
                    }
                    popupMenu.show()
                }
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
                    }
                }
            }
        }
    }

    interface CallBack {
        fun del(bookSource: BookSource)
        fun edit(bookSource: BookSource)
        fun update(vararg bookSource: BookSource)
        fun toTop(bookSource: BookSource)
        fun upOrder()
    }
}