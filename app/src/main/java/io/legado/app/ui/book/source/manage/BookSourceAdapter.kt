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
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.databinding.ItemBookSourceBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.model.Debug
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.invisible
import io.legado.app.utils.startActivity
import io.legado.app.utils.visible
import java.util.Collections


class BookSourceAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookSourcePart, ItemBookSourceBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<BookSourcePart>()
    private val finalMessageRegex = Regex("成功|失败")

    val selection: List<BookSourcePart>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallback = object : DiffUtil.ItemCallback<BookSourcePart>() {

        override fun areItemsTheSame(oldItem: BookSourcePart, newItem: BookSourcePart): Boolean {
            return oldItem.bookSourceUrl == newItem.bookSourceUrl
        }

        override fun areContentsTheSame(oldItem: BookSourcePart, newItem: BookSourcePart): Boolean {
            return oldItem.bookSourceName == newItem.bookSourceName
                    && oldItem.bookSourceGroup == newItem.bookSourceGroup
                    && oldItem.enabled == newItem.enabled
                    && oldItem.enabledExplore == newItem.enabledExplore
                    && oldItem.hasExploreUrl == newItem.hasExploreUrl
        }

        override fun getChangePayload(oldItem: BookSourcePart, newItem: BookSourcePart): Any? {
            val payload = Bundle()
            if (oldItem.bookSourceName != newItem.bookSourceName
                || oldItem.bookSourceGroup != newItem.bookSourceGroup
            ) {
                payload.putBoolean("upName", true)
            }
            if (oldItem.enabled != newItem.enabled) {
                payload.putBoolean("enabled", newItem.enabled)
            }
            if (oldItem.enabledExplore != newItem.enabledExplore ||
                oldItem.hasExploreUrl != newItem.hasExploreUrl
            ) {
                payload.putBoolean("upExplore", true)
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
        item: BookSourcePart,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val payload = payloads.getOrNull(0) as? Bundle
            if (payload == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbBookSource.text = item.getDisPlayNameGroup()
                swtEnabled.isChecked = item.enabled
                cbBookSource.isChecked = selected.contains(item)
                upCheckSourceMessage(binding, item)
                upShowExplore(ivExplore, item)
            } else {
                payload.keySet().map {
                    when (it) {
                        "enabled" -> swtEnabled.isChecked = payload.getBoolean("enabled")
                        "upName" -> cbBookSource.text = item.getDisPlayNameGroup()
                        "upExplore" -> upShowExplore(ivExplore, item)
                        "selected" -> cbBookSource.isChecked = selected.contains(item)
                        "checkSourceMessage" -> upCheckSourceMessage(binding, item)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemBookSourceBinding) {
        binding.apply {
            swtEnabled.setOnCheckedChangeListener { view, checked ->
                getItem(holder.layoutPosition)?.let {
                    if (view.isPressed) {
                        it.enabled = checked
                        callBack.enable(checked, it)
                    }
                }
            }
            cbBookSource.setOnCheckedChangeListener { view, checked ->
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

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.book_source_item)
        popupMenu.menu.findItem(R.id.menu_top).isVisible = callBack.sort == BookSourceSort.Default
        popupMenu.menu.findItem(R.id.menu_bottom).isVisible =
            callBack.sort == BookSourceSort.Default
        val qyMenu = popupMenu.menu.findItem(R.id.menu_enable_explore)
        if (!source.hasExploreUrl) {
            qyMenu.isVisible = false
        } else {
            if (source.enabledExplore) {
                qyMenu.setTitle(R.string.disable_explore)
            } else {
                qyMenu.setTitle(R.string.enable_explore)
            }
        }
        val loginMenu = popupMenu.menu.findItem(R.id.menu_login)
        loginMenu.isVisible = source.hasLoginUrl
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_login -> context.startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", source.bookSourceUrl)
                }

                R.id.menu_search -> callBack.searchBook(source)
                R.id.menu_debug_source -> callBack.debug(source)
                R.id.menu_del -> {
                    callBack.del(source)
                    selected.remove(source)
                }

                R.id.menu_enable_explore -> {
                    callBack.enableExplore(!source.enabledExplore, source)
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun upShowExplore(iv: ImageView, source: BookSourcePart) {
        when {
            !source.hasExploreUrl -> {
                iv.invisible()
            }

            source.enabledExplore -> {
                iv.setColorFilter(Color.GREEN)
                iv.visible()
                iv.contentDescription = context.getString(R.string.tag_explore_enabled)
            }

            else -> {
                iv.setColorFilter(Color.RED)
                iv.visible()
                iv.contentDescription = context.getString(R.string.tag_explore_disabled)
            }
        }
    }

    private fun upCheckSourceMessage(
        binding: ItemBookSourceBinding,
        item: BookSourcePart
    ) = binding.run {
        val msg = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
        ivDebugText.text = msg
        val isEmpty = msg.isEmpty()
        var isFinalMessage = msg.contains(finalMessageRegex)
        if (!Debug.isChecking && !isFinalMessage) {
            Debug.updateFinalMessage(item.bookSourceUrl, "校验失败")
            ivDebugText.text = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
            isFinalMessage = true
        }
        ivDebugText.visibility =
            if (!isEmpty) View.VISIBLE else View.GONE
        ivProgressBar.visibility =
            if (isFinalMessage || isEmpty || !Debug.isChecking) View.GONE else View.VISIBLE
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

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            val srcOrder = srcItem.customOrder
            srcItem.customOrder = targetItem.customOrder
            targetItem.customOrder = srcOrder
            movedItems.add(srcItem)
            movedItems.add(targetItem)
        }
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<BookSourcePart>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            val sortNumberSet = hashSetOf<Int>()
            movedItems.forEach {
                sortNumberSet.add(it.customOrder)
            }
            if (movedItems.size > sortNumberSet.size) {
                callBack.upOrder(getItems().mapIndexed { index, bookSourcePart ->
                    bookSourcePart.customOrder = if (callBack.sortAscending) index else -index
                    bookSourcePart
                })
            } else {
                callBack.upOrder(movedItems.toList())
            }
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<BookSourcePart>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<BookSourcePart> {
                return selected
            }

            override fun getItemId(position: Int): BookSourcePart {
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
        val sort: BookSourceSort
        val sortAscending: Boolean
        fun del(bookSource: BookSourcePart)
        fun edit(bookSource: BookSourcePart)
        fun toTop(bookSource: BookSourcePart)
        fun toBottom(bookSource: BookSourcePart)
        fun searchBook(bookSource: BookSourcePart)
        fun debug(bookSource: BookSourcePart)
        fun upOrder(items: List<BookSourcePart>)
        fun enable(enable: Boolean, bookSource: BookSourcePart)
        fun enableExplore(enable: Boolean, bookSource: BookSourcePart)
        fun upCountView()
    }
}
