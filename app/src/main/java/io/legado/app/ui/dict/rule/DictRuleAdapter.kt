package io.legado.app.ui.dict.rule

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.DictRule
import io.legado.app.databinding.ItemDictRuleBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.ColorUtils


class DictRuleAdapter(context: Context, var callBack: CallBack) :
    RecyclerAdapter<DictRule, ItemDictRuleBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<DictRule>()

    val selection: List<DictRule>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallBack = object : DiffUtil.ItemCallback<DictRule>() {

        override fun areItemsTheSame(oldItem: DictRule, newItem: DictRule): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: DictRule, newItem: DictRule): Boolean {
            if (oldItem.name != newItem.name) {
                return false
            }
            if (oldItem.enabled != newItem.enabled) {
                return false
            }
            return true
        }

        override fun getChangePayload(oldItem: DictRule, newItem: DictRule): Any? {
            val payload = Bundle()
            if (oldItem.name != newItem.name) {
                payload.putBoolean("upName", true)
            }
            if (oldItem.enabled != newItem.enabled) {
                payload.putBoolean("enabled", newItem.enabled)
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

    override fun getViewBinding(parent: ViewGroup): ItemDictRuleBinding {
        return ItemDictRuleBinding.inflate(inflater, parent, false)
    }

    override fun onCurrentListChanged() {
        callBack.upCountView()
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemDictRuleBinding,
        item: DictRule,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbName.text = item.name
                swtEnabled.isChecked = item.enabled
                cbName.isChecked = selected.contains(item)
            } else {
                for (i in payloads.indices) {
                    val bundle = payloads[i] as Bundle
                    bundle.keySet().map {
                        when (it) {
                            "selected" -> cbName.isChecked = selected.contains(item)
                            "upName" -> cbName.text = item.name
                            "enabled" -> swtEnabled.isChecked = item.enabled
                        }
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemDictRuleBinding) {
        binding.apply {
            swtEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    getItem(holder.layoutPosition)?.let {
                        it.enabled = isChecked
                        callBack.update(it)
                    }
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
            ivEdit.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
            ivDelete.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.delete(it)
                    selected.remove(it)
                }
            }
        }
    }

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.sortNumber == targetItem.sortNumber) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.sortNumber
                srcItem.sortNumber = targetItem.sortNumber
                targetItem.sortNumber = srcOrder
                movedItems.add(srcItem)
                movedItems.add(targetItem)
            }
        }
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = linkedSetOf<DictRule>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<DictRule>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<DictRule> {
                return selected
            }

            override fun getItemId(position: Int): DictRule {
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
        fun update(vararg rule: DictRule)
        fun delete(rule: DictRule)
        fun edit(rule: DictRule)
        fun upOrder()
        fun upCountView()
    }
}
