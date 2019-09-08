package io.legado.app.ui.booksource

import android.content.Context
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
            iv_edit_source.onClick { callBack.edit(item) }
            iv_top_source.onClick { callBack.topSource(item) }
            iv_del_source.onClick { callBack.del(item) }
        }
    }

    interface CallBack {
        fun del(bookSource: BookSource)
        fun edit(bookSource: BookSource)
        fun update(vararg bookSource: BookSource)
        fun topSource(bookSource: BookSource)
        fun upOrder()
    }
}