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
import io.legado.app.model.Debug
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.invisible
import io.legado.app.utils.startActivity
import io.legado.app.utils.visible
import java.util.*


class BookSourceAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<BookSource, ItemBookSourceBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<BookSource>()

    val selection: List<BookSource>
        get() {
            return getItems().filter {
                selected.contains(it)
            }
        }

    val diffItemCallback: DiffUtil.ItemCallback<BookSource>
        get() = object : DiffUtil.ItemCallback<BookSource>() {

            override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
                return oldItem.bookSourceUrl == newItem.bookSourceUrl
            }

            override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean {
                return oldItem.bookSourceName == newItem.bookSourceName
                        && oldItem.bookSourceGroup == newItem.bookSourceGroup
                        && oldItem.enabled == newItem.enabled
                        && oldItem.enabledExplore == newItem.enabledExplore
                        && oldItem.exploreUrl == newItem.exploreUrl
            }

            override fun getChangePayload(oldItem: BookSource, newItem: BookSource): Any? {
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
                    oldItem.exploreUrl != newItem.exploreUrl
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
        item: BookSource,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val payload = payloads.getOrNull(0) as? Bundle
            if (payload == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbBookSource.text = item.getDisPlayNameGroup()
                swtEnabled.isChecked = item.enabled
                cbBookSource.isChecked = selected.contains(item)
                ivDebugText.text = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
                ivDebugText.visibility =
                    if (ivDebugText.text.toString().isNotBlank()) View.VISIBLE else View.GONE
                upShowExplore(ivExplore, item)
            } else {
                payload.keySet().map {
                    when (it) {
                        "enabled" -> swtEnabled.isChecked = payload.getBoolean("enabled")
                        "upName" -> cbBookSource.text = item.getDisPlayNameGroup()
                        "upExplore" -> upShowExplore(ivExplore, item)
                        "selected" -> cbBookSource.isChecked = selected.contains(item)
                        "checkSourceMessage" -> {
                            ivDebugText.text = Debug.debugMessageMap[item.bookSourceUrl] ?: ""
                            val isEmpty = ivDebugText.text.toString().isEmpty()
                            var isFinalMessage =
                                ivDebugText.text.toString().contains(Regex("成功|失败"))
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
                        callBack.update(it)
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
        val loginMenu = popupMenu.menu.findItem(R.id.menu_login)
        loginMenu.isVisible = !source.loginUrl.isNullOrBlank()
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_login -> context.startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", source.bookSourceUrl)
                }
                R.id.menu_debug_source -> callBack.debug(source)
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
                iv.contentDescription = context.getString(R.string.tag_explore_enabled)
            }
            else -> {
                iv.setColorFilter(Color.RED)
                iv.visible()
                iv.contentDescription = context.getString(R.string.tag_explore_disabled)
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
        fun debug(bookSource: BookSource)
        fun upOrder()
        fun upCountView()
    }
}