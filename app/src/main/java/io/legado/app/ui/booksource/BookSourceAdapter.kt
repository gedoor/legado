package io.legado.app.ui.booksource

import android.content.Context
import android.view.Menu
import android.widget.PopupMenu
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

    private val selectedIds = linkedSetOf<String>()

    fun selectAll() {
        getItems().forEach {
            selectedIds.add(it.bookSourceUrl)
        }
        notifyItemRangeChanged(0, itemCount, 1)
    }

    fun revertSelection() {
        getItems().forEach {
            if (selectedIds.contains(it.bookSourceUrl)) {
                selectedIds.remove(it.bookSourceUrl)
            } else {
                selectedIds.add(it.bookSourceUrl)
            }
        }
        notifyItemRangeChanged(0, itemCount, 1)
    }

    fun getSelectionIds(): LinkedHashSet<String> {
        val selection = linkedSetOf<String>()
        getItems().map {
            if (selectedIds.contains(it.bookSourceUrl)) {
                selection.add(it.bookSourceUrl)
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
            this.setBackgroundColor(context.backgroundColor)
            if (item.bookSourceGroup.isNullOrEmpty()) {
                cb_book_source.text = item.bookSourceName
            } else {
                cb_book_source.text =
                    String.format("%s (%s)", item.bookSourceName, item.bookSourceGroup)
            }
            cb_book_source.isChecked = item.enabled
            cb_book_source.setOnClickListener {
                item.enabled = cb_book_source.isChecked
                callBack.update(item)
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