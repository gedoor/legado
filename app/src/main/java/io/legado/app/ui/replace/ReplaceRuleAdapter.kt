package io.legado.app.ui.replace

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.databinding.ItemReplaceRuleBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.ColorUtils


class ReplaceRuleAdapter(context: Context, var callBack: CallBack) :
    RecyclerAdapter<ReplaceRule, ItemReplaceRuleBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<ReplaceRule>()

    val selection: List<ReplaceRule>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallBack = object : DiffUtil.ItemCallback<ReplaceRule>() {

        override fun areItemsTheSame(oldItem: ReplaceRule, newItem: ReplaceRule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ReplaceRule, newItem: ReplaceRule): Boolean {
            if (oldItem.name != newItem.name) {
                return false
            }
            if (oldItem.group != newItem.group) {
                return false
            }
            if (oldItem.isEnabled != newItem.isEnabled) {
                return false
            }
            return true
        }

        override fun getChangePayload(oldItem: ReplaceRule, newItem: ReplaceRule): Any? {
            val payload = Bundle()
            if (oldItem.name != newItem.name
                || oldItem.group != newItem.group
            ) {
                payload.putBoolean("upName", true)
            }
            if (oldItem.isEnabled != newItem.isEnabled) {
                payload.putBoolean("enabled", newItem.isEnabled)
            }
            if (payload.isEmpty) {
                return null
            }
            return payload
        }
    }

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

    override fun getViewBinding(parent: ViewGroup): ItemReplaceRuleBinding {
        return ItemReplaceRuleBinding.inflate(inflater, parent, false)
    }

    override fun onCurrentListChanged() {
        callBack.upCountView()
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemReplaceRuleBinding,
        item: ReplaceRule,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbName.text = item.getDisplayNameGroup()
                swtEnabled.isChecked = item.isEnabled
                cbName.isChecked = selected.contains(item)
            } else {
                bundle.keySet().map {
                    when (it) {
                        "selected" -> cbName.isChecked = selected.contains(item)
                        "upName" -> cbName.text = item.getDisplayNameGroup()
                        "enabled" -> swtEnabled.isChecked = item.isEnabled
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemReplaceRuleBinding) {
        binding.apply {
            swtEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    getItem(holder.layoutPosition)?.let {
                        it.isEnabled = isChecked
                        callBack.update(it)
                    }
                }
            }
            ivEdit.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
            cbName.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    if (cbName.isChecked) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                }
                callBack.upCountView()
            }
            ivMenuMore.setOnClickListener {
                showMenu(ivMenuMore, holder.layoutPosition)
            }
        }
    }

    private fun showMenu(view: View, position: Int) {
        val item = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.replace_rule_item)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(item)
                R.id.menu_bottom -> callBack.toBottom(item)
                R.id.menu_del -> {
                    callBack.delete(item)
                    selected.remove(item)
                }
            }
            true
        }
        popupMenu.show()
    }

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.order == targetItem.order) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.order
                srcItem.order = targetItem.order
                targetItem.order = srcOrder
                movedItems.add(srcItem)
                movedItems.add(targetItem)
            }
        }
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = linkedSetOf<ReplaceRule>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<ReplaceRule>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<ReplaceRule> {
                return selected
            }

            override fun getItemId(position: Int): ReplaceRule {
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

    interface CallBack {
        fun update(vararg rule: ReplaceRule)
        fun delete(rule: ReplaceRule)
        fun edit(rule: ReplaceRule)
        fun toTop(rule: ReplaceRule)
        fun toBottom(rule: ReplaceRule)
        fun upOrder()
        fun upCountView()
    }
}
