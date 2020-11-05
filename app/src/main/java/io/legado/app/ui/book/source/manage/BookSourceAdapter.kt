package io.legado.app.ui.book.source.manage

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookSource
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback.Callback
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_book_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*

class BookSourceAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<BookSource>(context, R.layout.item_book_source),
    Callback {

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

    fun getSelection(): List<BookSource> {
        val selection = arrayListOf<BookSource>()
        getItems().map {
            if (selected.contains(it)) {
                selection.add(it)
            }
        }
        return selection.sortedBy { it.customOrder }
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
                cb_book_source.isChecked = selected.contains(item)
                upShowExplore(iv_explore, item)
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
                        "showExplore" -> upShowExplore(iv_explore, item)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            swt_enabled.setOnCheckedChangeListener { view, checked ->
                getItem(holder.layoutPosition)?.let {
                    if (view.isPressed) {
                        it.enabled = checked
                        callBack.update(it)
                    }
                }
            }
            cb_book_source.setOnCheckedChangeListener { view, checked ->
                getItem(holder.layoutPosition)?.let {
                    if (view.isPressed) {
                        if (checked) {
                            selected.add(it)
                        } else {
                            selected.remove(it)
                        }
                        callBack.upCountView()
                    }
                }
            }
            iv_edit.onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
            iv_menu_more.onClick {
                showMenu(iv_menu_more, holder.layoutPosition)
            }
        }
    }

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.book_source_item)
        val qyMenu = popupMenu.menu.findItem(R.id.menu_enable_explore)
        if (source.exploreUrl.isNullOrEmpty()) {
            qyMenu.isVisible = false
        } else {
            if (source.enabledExplore) {
                qyMenu.setTitle(R.string.disable_explore)
            } else {
                qyMenu.setTitle(R.string.enable_explore)
            }
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_del -> callBack.del(source)
                R.id.menu_enable_explore -> {
                    callBack.update(source.copy(enabledExplore = !source.enabledExplore))
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun upShowExplore(iv: ImageView, source: BookSource) {
        when {
            source.exploreUrl.isNullOrEmpty() -> {
                iv.invisible()
            }
            source.enabledExplore -> {
                iv.setColorFilter(Color.GREEN)
                iv.visible()
            }
            else -> {
                iv.setColorFilter(Color.RED)
                iv.visible()
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

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    fun initDragSelectTouchHelperCallback(): DragSelectTouchHelper.Callback {
        return object : DragSelectTouchHelper.AdvanceCallback<BookSource>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<BookSource> {
                return selected
            }

            override fun getItemId(position: Int): BookSource {
                return getItem(position)!!
            }

            override fun updateSelectState(position: Int, isSelected: Boolean): Boolean {
                getItem(position)?.let {
                    if (isSelected) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                    notifyItemChanged(position, bundleOf(Pair("selected", null)))
                    callBack.upCountView()
                    return true
                }
                return false
            }
        }
    }

    interface CallBack {
        fun del(bookSource: BookSource)
        fun edit(bookSource: BookSource)
        fun update(vararg bookSource: BookSource)
        fun toTop(bookSource: BookSource)
        fun toBottom(bookSource: BookSource)
        fun upOrder()
        fun upCountView()
    }
}