package io.legado.app.ui.rss.source.manage

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssSource
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import kotlinx.android.synthetic.main.item_rss_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*

class RssSourceAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssSource>(context, R.layout.item_rss_source),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<RssSource>()

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

    fun getSelection(): List<RssSource> {
        val selection = arrayListOf<RssSource>()
        getItems().forEach {
            if (selected.contains(it)) {
                selection.add(it)
            }
        }
        return selection.sortedBy { it.customOrder }
    }

    override fun convert(holder: ItemViewHolder, item: RssSource, payloads: MutableList<Any>) {
        with(holder.itemView) {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                this.setBackgroundColor(context.backgroundColor)
                if (item.sourceGroup.isNullOrEmpty()) {
                    cb_source.text = item.sourceName
                } else {
                    cb_source.text =
                        String.format("%s (%s)", item.sourceName, item.sourceGroup)
                }
                swt_enabled.isChecked = item.enabled
                cb_source.isChecked = selected.contains(item)
            } else {
                bundle.keySet().map {
                    when (it) {
                        "name", "group" ->
                            if (item.sourceGroup.isNullOrEmpty()) {
                                cb_source.text = item.sourceName
                            } else {
                                cb_source.text =
                                    String.format("%s (%s)", item.sourceName, item.sourceGroup)
                            }
                        "selected" -> cb_source.isChecked = selected.contains(item)
                        "enabled" -> swt_enabled.isChecked = item.enabled
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
            cb_source.setOnCheckedChangeListener { view, checked ->
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
        popupMenu.inflate(R.menu.rss_source_item)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_del -> callBack.del(source)
            }
            true
        }
        popupMenu.show()
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

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    fun initDragSelectTouchHelperCallback(): DragSelectTouchHelper.Callback {
        return object : DragSelectTouchHelper.AdvanceCallback<RssSource>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<RssSource> {
                return selected
            }

            override fun getItemId(position: Int): RssSource {
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
        fun del(source: RssSource)
        fun edit(source: RssSource)
        fun update(vararg source: RssSource)
        fun toTop(source: RssSource)
        fun toBottom(source: RssSource)
        fun upOrder()
        fun upCountView()
    }
}
