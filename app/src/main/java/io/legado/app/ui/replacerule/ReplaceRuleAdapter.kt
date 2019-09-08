package io.legado.app.ui.replacerule

import android.content.Context
import android.view.Menu
import android.widget.PopupMenu
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.theme.backgroundColor
import kotlinx.android.synthetic.main.item_replace_rule.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ReplaceRuleAdapter(context: Context, var callBack: CallBack) :
    SimpleRecyclerAdapter<ReplaceRule>(context, R.layout.item_replace_rule),
    ItemTouchCallback.OnItemTouchCallbackListener {

    val selectedIds = linkedSetOf<Long>()

    fun selectAll() {
        getItems().forEach {
            selectedIds.add(it.id)
        }
        notifyItemRangeChanged(0, itemCount, 1)
    }

    fun revertSelection() {
        getItems().forEach {
            if (selectedIds.contains(it.id)) {
                selectedIds.remove(it.id)
            } else {
                selectedIds.add(it.id)
            }
        }
        notifyItemRangeChanged(0, itemCount, 1)
    }

    override fun convert(holder: ItemViewHolder, item: ReplaceRule, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                this.setBackgroundColor(context.backgroundColor)
                cb_name.text = item.name
                swt_enabled.isChecked = item.isEnabled
                swt_enabled.onClick {
                    item.isEnabled = swt_enabled.isChecked
                    callBack.update(item)
                }
                iv_edit.onClick {
                    callBack.edit(item)
                }
                cb_name.isChecked = selectedIds.contains(item.id)
                cb_name.onClick {
                    if (cb_name.isChecked) {
                        selectedIds.add(item.id)
                    } else {
                        selectedIds.remove(item.id)
                    }
                }
                iv_menu_more.onClick {
                    val popupMenu = PopupMenu(context, it)
                    popupMenu.menu.add(Menu.NONE, R.id.menu_top, Menu.NONE, R.string.to_top)
                    popupMenu.menu.add(Menu.NONE, R.id.menu_del, Menu.NONE, R.string.delete)
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_top -> callBack.toTop(item)
                            R.id.menu_del -> callBack.delete(item)
                        }
                        true
                    }
                    popupMenu.show()
                }
            } else {
                when (payloads[0]) {
                    1 -> cb_name.isChecked = selectedIds.contains(item.id)
                    2 -> swt_enabled.isChecked = item.isEnabled
                }
            }
        }
    }

    override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.order == targetItem.order) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.order
                srcItem.order = targetItem.order
                targetItem.order = srcOrder
                callBack.update(srcItem, targetItem)
            }
        }
        return true
    }

    override fun onSwiped(adapterPosition: Int) {

    }

    interface CallBack {
        fun update(vararg rule: ReplaceRule)
        fun delete(rule: ReplaceRule)
        fun edit(rule: ReplaceRule)
        fun toTop(rule: ReplaceRule)
        fun upOrder()
    }
}
