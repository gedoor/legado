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

    override fun convert(holder: ItemViewHolder, item: ReplaceRule, payloads: MutableList<Any>) {
        with(holder.itemView) {
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
        }
    }

    override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.order == targetItem.order) {
                callBack.upOrder(getItems())
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
        fun upOrder(rules: List<ReplaceRule>)
    }
}
