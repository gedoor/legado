package io.legado.app.base.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.utils.buildMainHandler
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import splitties.views.onLongClick
import java.util.Collections

/**
 * Created by Invincible on 2017/11/24.
 *
 * 通用的adapter 可添加header，footer，以及不同类型item
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class RecyclerAdapter<ITEM, VB : ViewBinding>(protected val context: Context) :
    RecyclerView.Adapter<ItemViewHolder>() {

    val inflater: LayoutInflater = LayoutInflater.from(context)

    private val headerItems: SparseArray<(parent: ViewGroup) -> ViewBinding> by lazy { SparseArray() }
    private val footerItems: SparseArray<(parent: ViewGroup) -> ViewBinding> by lazy { SparseArray() }

    private val items: MutableList<ITEM> = mutableListOf()

    private var itemClickListener: ((holder: ItemViewHolder, item: ITEM) -> Unit)? = null
    private var itemLongClickListener: ((holder: ItemViewHolder, item: ITEM) -> Boolean)? = null

    private var diffJob: Coroutine<*>? = null

    var itemAnimation: ItemAnimation? = null

    fun setOnItemClickListener(listener: (holder: ItemViewHolder, item: ITEM) -> Unit) {
        itemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (holder: ItemViewHolder, item: ITEM) -> Boolean) {
        itemLongClickListener = listener
    }

    fun bindToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = this
    }

    @Synchronized
    fun addHeaderView(header: ((parent: ViewGroup) -> ViewBinding)) {
        kotlin.runCatching {
            val index = headerItems.size()
            headerItems.put(TYPE_HEADER_VIEW + headerItems.size(), header)
            notifyItemInserted(index)
        }
    }

    @Synchronized
    fun addFooterView(footer: ((parent: ViewGroup) -> ViewBinding)) {
        kotlin.runCatching {
            val index = getActualItemCount() + footerItems.size()
            footerItems.put(TYPE_FOOTER_VIEW + footerItems.size(), footer)
            notifyItemInserted(index)
        }
    }

    @Synchronized
    fun removeHeaderView(header: ((parent: ViewGroup) -> ViewBinding)) {
        kotlin.runCatching {
            val index = headerItems.indexOfValue(header)
            if (index >= 0) {
                headerItems.remove(index)
                notifyItemRemoved(index)
            }
        }
    }

    @Synchronized
    fun removeFooterView(footer: ((parent: ViewGroup) -> ViewBinding)) {
        kotlin.runCatching {
            val index = footerItems.indexOfValue(footer)
            if (index >= 0) {
                footerItems.remove(index)
                notifyItemRemoved(getActualItemCount() + index - 2)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    fun setItems(items: List<ITEM>?) {
        kotlin.runCatching {
            if (this.items.isNotEmpty()) {
                this.items.clear()
            }
            if (items != null) {
                this.items.addAll(items)
            }
            notifyDataSetChanged()
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun setItems(
        items: List<ITEM>?,
        itemCallback: DiffUtil.ItemCallback<ITEM>,
        skipDiff: Boolean = false
    ) {
        kotlin.runCatching {
            val oldItems = this.items.toList()
            val callback = object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return itemCount
                }

                override fun getNewListSize(): Int {
                    return (items?.size ?: 0) + getHeaderCount() + getFooterCount()
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldItem = oldItems.getOrNull(oldItemPosition - getHeaderCount())
                        ?: return true
                    val newItem = items?.getOrNull(newItemPosition - getHeaderCount())
                        ?: return true
                    return itemCallback.areItemsTheSame(oldItem, newItem)
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    val oldItem = oldItems.getOrNull(oldItemPosition - getHeaderCount())
                        ?: return true
                    val newItem = items?.getOrNull(newItemPosition - getHeaderCount())
                        ?: return true
                    return itemCallback.areContentsTheSame(oldItem, newItem)
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                    val oldItem = oldItems.getOrNull(oldItemPosition - getHeaderCount())
                        ?: return null
                    val newItem = items?.getOrNull(newItemPosition - getHeaderCount())
                        ?: return null
                    return itemCallback.getChangePayload(oldItem, newItem)
                }
            }
            diffJob?.cancel()
            diffJob = Coroutine.async {
                val diffResult = if (skipDiff) withTimeoutOrNull(500L) {
                    DiffUtil.calculateDiff(callback)
                } else {
                    DiffUtil.calculateDiff(callback)
                }
                ensureActive()
                handler.post {
                    if (diffResult == null) {
                        setItems(items)
                        return@post
                    }
                    if (this@RecyclerAdapter.items.isNotEmpty()) {
                        this@RecyclerAdapter.items.clear()
                    }
                    if (items != null) {
                        this@RecyclerAdapter.items.addAll(items)
                    }
                    diffResult.dispatchUpdatesTo(this@RecyclerAdapter)
                    onCurrentListChanged()
                }
            }
        }
    }

    @Synchronized
    fun setItem(position: Int, item: ITEM) {
        kotlin.runCatching {
            val oldSize = getActualItemCount()
            if (position in 0 until oldSize) {
                this.items[position] = item
                notifyItemChanged(position + getHeaderCount())
            }
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun addItem(item: ITEM) {
        kotlin.runCatching {
            val oldSize = getActualItemCount()
            if (this.items.add(item)) {
                notifyItemInserted(oldSize + getHeaderCount())
            }
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun addItems(position: Int, newItems: List<ITEM>) {
        kotlin.runCatching {
            if (this.items.addAll(position, newItems)) {
                notifyItemRangeInserted(position + getHeaderCount(), newItems.size)
            }
            onCurrentListChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    fun addItems(newItems: List<ITEM>) {
        kotlin.runCatching {
            val oldSize = getActualItemCount()
            if (this.items.addAll(newItems)) {
                if (oldSize == 0 && getHeaderCount() == 0) {
                    notifyDataSetChanged()
                } else {
                    notifyItemRangeInserted(oldSize + getHeaderCount(), newItems.size)
                }
            }
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun removeItem(position: Int) {
        kotlin.runCatching {
            if (this.items.removeAt(position) != null) {
                notifyItemRemoved(position + getHeaderCount())
            }
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun removeItem(item: ITEM) {
        kotlin.runCatching {
            if (this.items.remove(item)) {
                notifyItemRemoved(this.items.indexOf(item) + getHeaderCount())
            }
            onCurrentListChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    fun removeItems(items: List<ITEM>) {
        kotlin.runCatching {
            if (this.items.removeAll(items)) {
                notifyDataSetChanged()
            }
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun swapItem(oldPosition: Int, newPosition: Int) {
        kotlin.runCatching {
            val size = getActualItemCount()
            if (oldPosition in 0 until size && newPosition in 0 until size) {
                val srcPosition = oldPosition + getHeaderCount()
                val targetPosition = newPosition + getHeaderCount()
                Collections.swap(this.items, srcPosition, targetPosition)
                notifyItemMoved(srcPosition, targetPosition)
            }
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun updateItem(item: ITEM) {
        kotlin.runCatching {
            val index = this.items.indexOf(item)
            if (index >= 0) {
                this.items[index] = item
                notifyItemChanged(index)
            }
            onCurrentListChanged()
        }
    }

    @Synchronized
    fun updateItem(position: Int, payload: Any) {
        kotlin.runCatching {
            val size = getActualItemCount()
            if (position in 0 until size) {
                notifyItemChanged(position + getHeaderCount(), payload)
            }
        }
    }

    @Synchronized
    fun updateItems(fromPosition: Int, toPosition: Int, payloads: Any) {
        kotlin.runCatching {
            val size = getActualItemCount()
            if (fromPosition in 0 until size && toPosition in 0 until size) {
                notifyItemRangeChanged(
                    fromPosition + getHeaderCount(),
                    toPosition - fromPosition + 1,
                    payloads
                )
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    fun clearItems() {
        kotlin.runCatching {
            this.items.clear()
            notifyDataSetChanged()
            onCurrentListChanged()
        }
    }

    fun isEmpty() = items.isEmpty()

    fun isNotEmpty() = items.isNotEmpty()

    /**
     * 除去header和footer
     */
    fun getActualItemCount() = items.size


    fun getHeaderCount() = headerItems.size()


    fun getFooterCount() = footerItems.size()

    fun getItem(position: Int): ITEM? = items.getOrNull(position)

    fun getItemByLayoutPosition(position: Int) = items.getOrNull(getActualPosition(position))

    fun getItems(): List<ITEM> = items.toList()

    protected open fun getItemViewType(item: ITEM, position: Int) = 0

    /**
     * grid 模式下使用
     */
    protected open fun getSpanSize(viewType: Int, position: Int) = 1

    final override fun getItemCount() = getActualItemCount() + getHeaderCount() + getFooterCount()

    final override fun getItemViewType(position: Int) = when {
        isHeader(position) -> TYPE_HEADER_VIEW + position
        isFooter(position) -> TYPE_FOOTER_VIEW + position - getActualItemCount() - getHeaderCount()
        else -> getItemByLayoutPosition(position)?.let {
            getItemViewType(it, getActualPosition(position))
        } ?: 0
    }

    open fun onCurrentListChanged() {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when {
        viewType < TYPE_HEADER_VIEW + getHeaderCount() -> {
            ItemViewHolder(headerItems.get(viewType).invoke(parent))
        }

        viewType >= TYPE_FOOTER_VIEW -> {
            ItemViewHolder(footerItems.get(viewType).invoke(parent))
        }

        else -> {
            val holder = ItemViewHolder(getViewBinding(parent))

            @Suppress("UNCHECKED_CAST")
            registerListener(holder, (holder.binding as VB))

            if (itemClickListener != null) {
                holder.itemView.setOnClickListener {
                    getItemByLayoutPosition(holder.layoutPosition)?.let {
                        itemClickListener?.invoke(holder, it)
                    }
                }
            }

            if (itemLongClickListener != null) {
                holder.itemView.onLongClick {
                    getItemByLayoutPosition(holder.layoutPosition)?.let {
                        itemLongClickListener?.invoke(holder, it)
                    }
                }
            }

            holder
        }
    }

    protected abstract fun getViewBinding(parent: ViewGroup): VB

    final override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {}

    @Suppress("UNCHECKED_CAST")
    final override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (!isHeader(holder.layoutPosition) && !isFooter(holder.layoutPosition)) {
            getItemByLayoutPosition(holder.layoutPosition)?.let {
                convert(holder, (holder.binding as VB), it, payloads)
            }
        }
    }

    override fun onViewAttachedToWindow(holder: ItemViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (!isHeader(holder.layoutPosition) && !isFooter(holder.layoutPosition)) {
            addAnimation(holder)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val manager = recyclerView.layoutManager
        if (manager is GridLayoutManager) {
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return getSpanSize(getItemViewType(position), position)
                }
            }
        }
    }

    private fun isHeader(position: Int) = position < getHeaderCount()

    private fun isFooter(position: Int) = position >= getActualItemCount() + getHeaderCount()

    private fun getActualPosition(position: Int) = position - getHeaderCount()

    private fun addAnimation(holder: ItemViewHolder) {
        itemAnimation?.let {
            if (it.itemAnimEnabled) {
                if (!it.itemAnimFirstOnly || holder.layoutPosition > it.itemAnimStartPosition) {
                    startAnimation(holder, it)
                    it.itemAnimStartPosition = holder.layoutPosition
                }
            }
        }
    }

    protected open fun startAnimation(holder: ItemViewHolder, item: ItemAnimation) {
        item.itemAnimation?.let {
            for (anim in it.getAnimators(holder.itemView)) {
                anim.setDuration(item.itemAnimDuration).start()
                anim.interpolator = item.itemAnimInterpolator
            }
        }
    }

    /**
     * 如果使用了事件回调,回调里不要直接使用item,会出现不更新的问题,
     * 使用getItem(holder.layoutPosition)来获取item
     */
    abstract fun convert(
        holder: ItemViewHolder,
        binding: VB,
        item: ITEM,
        payloads: MutableList<Any>
    )

    /**
     * 注册事件
     */
    abstract fun registerListener(holder: ItemViewHolder, binding: VB)

    companion object {
        private const val TYPE_HEADER_VIEW = Int.MIN_VALUE
        private const val TYPE_FOOTER_VIEW = Int.MAX_VALUE - 999
        private val handler by lazy { buildMainHandler() }
    }

}




