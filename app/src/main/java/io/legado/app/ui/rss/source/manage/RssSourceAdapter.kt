package io.legado.app.ui.rss.source.manage

import android.content.Context
import android.view.Menu
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssSource
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.theme.backgroundColor
import kotlinx.android.synthetic.main.item_rss_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*

class RssSourceAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssSource>(context, R.layout.item_rss_source),
    ItemTouchCallback.OnItemTouchCallbackListener {

    private val selectedIds = linkedSetOf<String>()

    fun selectAll() {
        getItems().forEach {
            selectedIds.add(it.sourceUrl)
        }
        notifyItemRangeChanged(0, itemCount, 1)
    }

    fun revertSelection() {
        getItems().forEach {
            if (selectedIds.contains(it.sourceUrl)) {
                selectedIds.remove(it.sourceUrl)
            } else {
                selectedIds.add(it.sourceUrl)
            }
        }
        notifyItemRangeChanged(0, itemCount, 1)
    }

    fun getSelectionIds(): LinkedHashSet<String> {
        val selection = linkedSetOf<String>()
        getItems().forEach {
            if (selectedIds.contains(it.sourceUrl)) {
                selection.add(it.sourceUrl)
            }
        }
        return selection
    }

    override fun convert(holder: ItemViewHolder, item: RssSource, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                this.setBackgroundColor(context.backgroundColor)
                if (item.sourceGroup.isNullOrEmpty()) {
                    cb_source.text = item.sourceName
                } else {
                    cb_source.text =
                        String.format("%s (%s)", item.sourceName, item.sourceGroup)
                }
                swt_enabled.isChecked = item.enabled
                swt_enabled.onClick {
                    item.enabled = swt_enabled.isChecked
                    callBack.update(item)
                }
                cb_source.isChecked = selectedIds.contains(item.sourceUrl)
                cb_source.setOnClickListener {
                    if (cb_source.isChecked) {
                        selectedIds.add(item.sourceUrl)
                    } else {
                        selectedIds.remove(item.sourceUrl)
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
                when (payloads[0]) {
                    1 -> cb_source.isChecked = selectedIds.contains(item.sourceUrl)
                    2 -> swt_enabled.isChecked = item.enabled
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

    private val movedItems = hashSetOf<RssSource>()

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    interface CallBack {
        fun del(source: RssSource)
        fun edit(source: RssSource)
        fun update(vararg source: RssSource)
        fun toTop(source: RssSource)
        fun upOrder()
    }
}
