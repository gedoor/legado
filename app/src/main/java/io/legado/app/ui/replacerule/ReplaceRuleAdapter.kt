package io.legado.app.ui.replacerule

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.ItemTouchCallback
import kotlinx.android.synthetic.main.item_replace_rule.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ReplaceRuleAdapter(context: Context, var callBack: CallBack) :
    PagedListAdapter<ReplaceRule, ReplaceRuleAdapter.MyViewHolder>(DIFF_CALLBACK),
    ItemTouchCallback.OnItemTouchCallbackListener {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ReplaceRule>() {
            override fun areItemsTheSame(oldItem: ReplaceRule, newItem: ReplaceRule): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ReplaceRule, newItem: ReplaceRule): Boolean =
                oldItem.id == newItem.id
                        && oldItem.pattern == newItem.pattern
                        && oldItem.replacement == newItem.replacement
                        && oldItem.isRegex == newItem.isRegex
                        && oldItem.isEnabled == newItem.isEnabled
                        && oldItem.scope == newItem.scope
        }
    }

    override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
        return true
    }

    override fun onSwiped(adapterPosition: Int) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_replace_rule,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, pos: Int) {
        getItem(pos)?.let { holder.bind(it, callBack) }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(rule: ReplaceRule, callBack: CallBack) = with(itemView) {
            cb_name.text = rule.name
            swt_enabled.isChecked = rule.isEnabled
            swt_enabled.onClick {
                rule.isEnabled = swt_enabled.isChecked
                callBack.update(rule)
            }
            iv_menu_more.onClick {
                val popupMenu = PopupMenu(context, it)
                popupMenu.menu.add(Menu.NONE, R.id.menu_edit, Menu.NONE, R.string.edit)
                popupMenu.menu.add(Menu.NONE, R.id.menu_del, Menu.NONE, R.string.delete)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit -> callBack.edit(rule)
                        R.id.menu_del -> callBack.delete(rule)
                    }
                    true
                }
                popupMenu.show()
            }
        }
    }

    interface CallBack {
        fun update(rule: ReplaceRule)
        fun delete(rule: ReplaceRule)
        fun edit(rule: ReplaceRule)
    }
}
