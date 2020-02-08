package io.legado.app.ui.rss.source.manage

import android.content.Context
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

    private val selected = linkedSetOf<RssSource>()

    fun selectAll() {
        getItems().forEach {
            selected.add(it)
        }
        notifyItemRangeChanged(0, itemCount, 1)
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
        notifyItemRangeChanged(0, itemCount, 1)
        callBack.upCountView()
    }

    fun getSelection(): LinkedHashSet<RssSource> {
        val selection = linkedSetOf<RssSource>()
        getItems().forEach {
            if (selected.contains(it)) {
                selection.add(it)
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
                cb_source.isChecked = selected.contains(item)
                cb_source.setOnClickListener {
                    if (cb_source.isChecked) {
                        selected.add(item)
                    } else {
                        selected.remove(item)
                    }
                    callBack.upCountView()
                }
                iv_edit.onClick { callBack.edit(item) }
                iv_menu_more.onClick {
                    val popupMenu = PopupMenu(context, it)
                    popupMenu.inflate(R.menu.rss_source_item)
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
                    1 -> cb_source.isChecked = selected.contains(item)
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
        fun upCountView()
    }
}
