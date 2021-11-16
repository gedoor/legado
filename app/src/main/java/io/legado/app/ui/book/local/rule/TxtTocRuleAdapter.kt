package io.legado.app.ui.book.local.rule

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.databinding.ItemTxtTocRuleBinding

class TxtTocRuleAdapter(context: Context, private val callback: Callback) :
    RecyclerAdapter<TxtTocRule, ItemTxtTocRuleBinding>(context) {

    private val selected = linkedSetOf<TxtTocRule>()

    val selection: List<TxtTocRule>
        get() {
            val selection = arrayListOf<TxtTocRule>()
            getItems().map {
                if (selected.contains(it)) {
                    selection.add(it)
                }
            }
            return selection.sortedBy { it.serialNumber }
        }

    override fun getViewBinding(parent: ViewGroup): ItemTxtTocRuleBinding {
        return ItemTxtTocRuleBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemTxtTocRuleBinding,
        item: TxtTocRule,
        payloads: MutableList<Any>
    ) {
        binding.cbSource.text = item.name
        binding.swtEnabled.isChecked = item.enable
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemTxtTocRuleBinding) {
        binding.cbSource.setOnCheckedChangeListener { buttonView, isChecked ->
            getItem(holder.layoutPosition)?.let {
                if (buttonView.isPressed) {
                    if (isChecked) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                    callback.upCountView()
                }
            }
        }
        binding.swtEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
            getItem(holder.layoutPosition)?.let {
                if (buttonView.isPressed) {
                    it.enable = isChecked
                }
            }
        }
        binding.ivEdit.setOnClickListener {

        }
    }

    interface Callback {
        fun del(source: TxtTocRule)
        fun edit(source: TxtTocRule)
        fun update(vararg source: TxtTocRule)
        fun toTop(source: TxtTocRule)
        fun toBottom(source: TxtTocRule)
        fun upOrder()
        fun upCountView()
    }

}