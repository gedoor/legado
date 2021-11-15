package io.legado.app.ui.book.local.rule

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.databinding.ItemTxtTocRuleBinding

class TxtTocRuleAdapter(context: Context) :
    RecyclerAdapter<TxtTocRule, ItemTxtTocRuleBinding>(context) {

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
        TODO("Not yet implemented")
    }

}