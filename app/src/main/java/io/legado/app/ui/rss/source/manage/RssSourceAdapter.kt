package io.legado.app.ui.rss.source.manage

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
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.ItemRssSourceBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.ColorUtils
import java.util.*


class RssSourceAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssSource, ItemRssSourceBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<RssSource>()

    val selection: List<RssSource>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallback = object : DiffUtil.ItemCallback<RssSource>() {

        override fun areItemsTheSame(oldItem: RssSource, newItem: RssSource): Boolean {
            return oldItem.sourceUrl == newItem.sourceUrl
        }

        override fun areContentsTheSame(oldItem: RssSource, newItem: RssSource): Boolean {
            return oldItem.sourceName == newItem.sourceName
                    && oldItem.sourceGroup == newItem.sourceGroup
                    && oldItem.enabled == newItem.enabled
        }

        override fun getChangePayload(oldItem: RssSource, newItem: RssSource): Any? {
            val payload = Bundle()
            if (oldItem.sourceName != newItem.sourceName
                || oldItem.sourceGroup != newItem.sourceGroup
            ) {
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

    override fun getViewBinding(parent: ViewGroup): ItemRssSourceBinding {
        return ItemRssSourceBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRssSourceBinding,
        item: RssSource,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbSource.text = item.getDisplayNameGroup()
                swtEnabled.isChecked = item.enabled
                cbSource.isChecked = selected.contains(item)
            } else {
                bundle.keySet().map {
                    when (it) {
                        "upName" -> cbSource.text = item.getDisplayNameGroup()
                        "enabled" -> swtEnabled.isChecked = bundle.getBoolean("enabled")
                        "selected" -> cbSource.isChecked = selected.contains(item)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRssSourceBinding) {
        binding.apply {
            swtEnabled.setOnCheckedChangeListener { view, checked ->
                if (view.isPressed) {
                    getItem(holder.layoutPosition)?.let {
                        if (view.isPressed) {
                            it.enabled = checked
                            callBack.update(it)
                        }
                    }
                }
            }
            cbSource.setOnCheckedChangeListener { view, checked ->
                if (view.isPressed) {
                    getItem(holder.layoutPosition)?.let {
                        if (view.isPressed) {
                            if (checked) {
                                selected.add(it)
                            } else {
                                selected.remove(it)
                            }
                            callBack.upCountView()
                        }
                    }
                }
            }
            ivEdit.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
            ivMenuMore.setOnClickListener {
                showMenu(ivMenuMore, holder.layoutPosition)
            }
        }
    }

    override fun onCurrentListChanged() {
        callBack.upCountView()
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

    fun checkSelectedInterval() {
        val selectedPosition = linkedSetOf<Int>()
        getItems().forEachIndexed { index, it ->
            if (selected.contains(it)) {
                selectedPosition.add(index)
            }
        }
        val minPosition = Collections.min(selectedPosition)
        val maxPosition = Collections.max(selectedPosition)
        val itemCount = maxPosition - minPosition + 1
        for (i in minPosition..maxPosition) {
            getItem(i)?.let {
                selected.add(it)
            }
        }
        notifyItemRangeChanged(minPosition, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.rss_source_item)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_del -> {
                    callBack.del(source)
                    selected.remove(source)
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
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<RssSource>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<RssSource>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<RssSource> {
                return selected
            }

            override fun getItemId(position: Int): RssSource {
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
        fun del(source: RssSource)
        fun edit(source: RssSource)
        fun update(vararg source: RssSource)
        fun toTop(source: RssSource)
        fun toBottom(source: RssSource)
        fun upOrder()
        fun upCountView()
    }
}
