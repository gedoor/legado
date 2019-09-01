package io.legado.app.ui.replacerule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.ReplaceRule
import kotlinx.android.synthetic.main.item_relace_rule.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ReplaceRuleAdapter(context: Context, var callBack: CallBack) :
    PagedListAdapter<ReplaceRule, ReplaceRuleAdapter.MyViewHolder>(DIFF_CALLBACK) {

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

    init {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_relace_rule, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, pos: Int) {
        getItem(pos)?.let { holder.bind(it, callBack) }
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(rule: ReplaceRule, listener: CallBack?) = with(itemView) {
            tv_name.text = rule.name
            swt_enabled.isChecked = rule.isEnabled
            // divider.isGone          = hideDivider
            iv_delete.isGone = true
            iv_edit.isGone = true
            // iv_delete.onClick { listener?.delete(rule) }
            // iv_edit.onClick { listener?.edit(rule) }
            swt_enabled.onClick {
                rule.isEnabled = swt_enabled.isChecked
                listener?.update(rule)
            }
        }
    }

    interface CallBack {
        fun update(rule: ReplaceRule)
        fun delete(rule: ReplaceRule)
        fun edit(rule: ReplaceRule)
    }
}
