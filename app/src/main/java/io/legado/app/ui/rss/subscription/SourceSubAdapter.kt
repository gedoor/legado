package io.legado.app.ui.rss.subscription

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SourceSub
import io.legado.app.databinding.ItemSourceSubBinding
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*

class SourceSubAdapter(context: Context, val callBack: Callback) :
    SimpleRecyclerAdapter<SourceSub, ItemSourceSubBinding>(context),
    ItemTouchCallback.Callback {

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
            showMenu(binding.ivMenuMore, holder.layoutPosition)
        }
    }

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.source_sub_item)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_del -> callBack.delSubscription(source)
            }
            true
        }
        popupMenu.show()
    }

    override fun getViewBinding(parent: ViewGroup): ItemSourceSubBinding {
        return ItemSourceSubBinding.inflate(inflater, parent, false)
    }

    override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.customOrder == targetItem.customOrder) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.customOrder
                srcItem.customOrder = targetItem.customOrder
                targetItem.customOrder = srcOrder
                movedItems.add(srcItem)
                movedItems.add(targetItem)
            }
        }
        Collections.swap(getItems(), srcPosition, targetPosition)
        notifyItemMoved(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<SourceSub>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.updateSourceSub(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    interface Callback {
        fun openSubscription(sourceSub: SourceSub)
        fun editSubscription(sourceSub: SourceSub)
        fun delSubscription(sourceSub: SourceSub)
        fun updateSourceSub(vararg sourceSub: SourceSub)
        fun upOrder()
    }

}