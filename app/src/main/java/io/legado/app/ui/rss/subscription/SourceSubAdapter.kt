package io.legado.app.ui.rss.subscription

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SourceSub
import io.legado.app.databinding.ItemSourceSubBinding
import org.jetbrains.anko.sdk27.listeners.onClick

class SourceSubAdapter(context: Context, val callBack: Callback) :
    SimpleRecyclerAdapter<SourceSub, ItemSourceSubBinding>(context) {


    override fun convert(
        holder: ItemViewHolder,
        binding: ItemSourceSubBinding,
        item: SourceSub,
        payloads: MutableList<Any>
    ) {
        binding.tvType.text = SourceSub.Type.values()[item.type].name
        binding.tvName.text = item.name
        binding.tvUrl.text = item.url
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemSourceSubBinding) {
        binding.root.onClick {
            callBack.openSubscription(getItem(holder.layoutPosition)!!)
        }
        binding.ivEdit.onClick {
            callBack.editSubscription(getItem(holder.layoutPosition)!!)
        }
        binding.ivMenuMore.onClick {

        }
    }

    override fun getViewBinding(parent: ViewGroup): ItemSourceSubBinding {
        return ItemSourceSubBinding.inflate(inflater, parent, false)
    }

    interface Callback {
        fun openSubscription(sourceSub: SourceSub)
        fun editSubscription(sourceSub: SourceSub)
        fun delSubscription(sourceSub: SourceSub)
    }

}