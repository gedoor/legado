package io.legado.app.ui.replacerule

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.widget.PopupMenu
import androidx.core.os.bundleOf
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

    private val selected = linkedSetOf<ReplaceRule>()

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

    fun getSelection(): LinkedHashSet<ReplaceRule> {
        val selection = linkedSetOf<ReplaceRule>()
        getItems().map {
            if (selected.contains(it)) {
                selection.add(it)
            }
        }
        return selection
    }

    override fun convert(holder: ItemViewHolder, item: ReplaceRule, payloads: MutableList<Any>) {
        with(holder.itemView) {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                this.setBackgroundColor(context.backgroundColor)
                if (item.group.isNullOrEmpty()) {
                    cb_name.text = item.name
                } else {
                    cb_name.text =
                        String.format("%s (%s)", item.name, item.group)
                }
                swt_enabled.isChecked = item.isEnabled
                swt_enabled.onClick {
                    item.isEnabled = swt_enabled.isChecked
                    callBack.update(item)
                }
                iv_edit.onClick {
                    callBack.edit(item)
                }
                cb_name.isChecked = selected.contains(item)
                cb_name.onClick {
                    if (cb_name.isChecked) {
                        selected.add(item)
                    } else {
                        selected.remove(item)
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
                bundle.keySet().map {
                    when (it) {
                        "selected" -> cb_name.isChecked = selected.contains(item)
                        "name", "group" ->
                            if (item.group.isNullOrEmpty()) {
                                cb_name.text = item.name
                            } else {
                                cb_name.text =
                                    String.format("%s (%s)", item.name, item.group)
                            }
                        "enabled" -> swt_enabled.isChecked = item.isEnabled
                    }
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
