package io.legado.app.ui.book.source.manage

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ItemBookSourceBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback.Callback
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import org.jetbrains.anko.sdk27.listeners.onClick

class BookSourceAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookSource, ItemBookSourceBinding>(context),
    Callback {

    private val selected = linkedSetOf<BookSource>()

    val diffItemCallback: DiffUtil.ItemCallback<BookSource>
        get() = object : DiffUtil.ItemCallback<BookSource>() {

            override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
                return oldItem.bookSourceUrl == newItem.bookSourceUrl
            }

            override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
                if (oldItem.bookSourceName != newItem.bookSourceName)
                    return false
                if (oldItem.bookSourceGroup != newItem.bookSourceGroup)
                    return false
                if (oldItem.enabled != newItem.enabled)
                    return false
                if (oldItem.enabledExplore != newItem.enabledExplore
                    || oldItem.exploreUrl != newItem.exploreUrl
                ) {
                    return false
                }
                return true
            }

            override fun getChangePayload(oldItem: BookSource, newItem: BookSource): Any? {
                val payload = Bundle()
                if (oldItem.bookSourceName != newItem.bookSourceName) {
                    payload.putString("name", newItem.bookSourceName)
                }
                if (oldItem.bookSourceGroup != newItem.bookSourceGroup) {
                    payload.putString("group", newItem.bookSourceGroup)
                }
                if (oldItem.enabled != newItem.enabled) {
                    payload.putBoolean("enabled", newItem.enabled)
                }
                if (oldItem.enabledExplore != newItem.enabledExplore
                    || oldItem.exploreUrl != newItem.exploreUrl
                ) {
                    payload.putBoolean("showExplore", true)
                }
                if (payload.isEmpty) {
                    return null
                }
                return payload
            }
        }

    override fun getViewBinding(parent: ViewGroup): ItemBookSourceBinding {
        return ItemBookSourceBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemBookSourceBinding,
        item: BookSource,
        payloads: MutableList<Any>
    ) {
        with(binding) {
            val payload = payloads.getOrNull(0) as? Bundle
            if (payload == null) {
                root.setBackgroundColor(context.backgroundColor)
                if (item.bookSourceGroup.isNullOrEmpty()) {
                    cbBookSource.text = item.bookSourceName
                } else {
                    cbBookSource.text =
                        String.format("%s (%s)", item.bookSourceName, item.bookSourceGroup)
                }
                swtEnabled.isChecked = item.enabled
                cbBookSource.isChecked = selected.contains(item)
                upShowExplore(ivExplore, item)
            } else {
                payload.keySet().map {
                    when (it) {
                        "selected" -> cbBookSource.isChecked = selected.contains(item)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookSourceBinding) {
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
            cbBookSource.setOnCheckedChangeListener { view, checked ->
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
            ivEdit.onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.edit(it)
                }
            }
            ivMenuMore.onClick {
                showMenu(ivMenuMore, holder.layoutPosition)
            }
        }
    }

    override fun onCurrentListChanged() {
        callBack.upCountView()
    }

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.book_source_item)
        val qyMenu = popupMenu.menu.findItem(R.id.menu_enable_explore)
        if (source.exploreUrl.isNullOrEmpty()) {
            qyMenu.isVisible = false
        } else {
            if (source.enabledExplore) {
                qyMenu.setTitle(R.string.disable_explore)
            } else {
                qyMenu.setTitle(R.string.enable_explore)
            }
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_del -> callBack.del(source)
                R.id.menu_enable_explore -> {
                    callBack.update(source.copy(enabledExplore = !source.enabledExplore))
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun upShowExplore(iv: ImageView, source: BookSource) {
        when {
            source.exploreUrl.isNullOrEmpty() -> {
                iv.invisible()
            }
            source.enabledExplore -> {
                iv.setColorFilter(Color.GREEN)
                iv.visible()
            }
            else -> {
                iv.setColorFilter(Color.RED)
                iv.visible()
            }
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

    fun getSelection(): List<BookSource> {
        val selection = arrayListOf<BookSource>()
        getItems().map {
            if (selected.contains(it)) {
                selection.add(it)
            }
        }
        return selection.sortedBy { it.customOrder }
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

    private val movedItems = hashSetOf<BookSource>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<BookSource>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<BookSource> {
                return selected
            }

            override fun getItemId(position: Int): BookSource {
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
        fun del(bookSource: BookSource)
        fun edit(bookSource: BookSource)
        fun update(vararg bookSource: BookSource)
        fun toTop(bookSource: BookSource)
        fun toBottom(bookSource: BookSource)
        fun upOrder()
        fun upCountView()
    }
}