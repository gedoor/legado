package io.legado.app.base.adapter

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Invincible on 2017/11/24.
 *
 * 通用的adapter 可添加header，footer，以及不同类型item
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
abstract class CommonRecyclerAdapter<ITEM, VB : ViewBinding>(protected val context: Context) :
    RecyclerView.Adapter<ItemViewHolder>() {

    constructor(context: Context, vararg delegates: ItemViewDelegate<ITEM, VB>) : this(context) {
        addItemViewDelegates(*delegates)
    }

    constructor(
        context: Context,
        vararg delegates: Pair<Int, ItemViewDelegate<ITEM, VB>>
    ) : this(context) {
        addItemViewDelegates(*delegates)
    }

    val inflater: LayoutInflater = LayoutInflater.from(context)

    private val headerItems: SparseArray<(parent: ViewGroup) -> ViewBinding> by lazy { SparseArray() }
    private val footerItems: SparseArray<(parent: ViewGroup) -> ViewBinding> by lazy { SparseArray() }

    private val itemDelegates: HashMap<Int, ItemViewDelegate<ITEM, VB>> = hashMapOf()

    private val asyncListDiffer: AsyncListDiffer<ITEM> by lazy {
        AsyncListDiffer(this, diffItemCallback)
    }

    private val lock = Object()

    private var itemClickListener: ((holder: ItemViewHolder, item: ITEM) -> Unit)? = null
    private var itemLongClickListener: ((holder: ItemViewHolder, item: ITEM) -> Boolean)? = null

    var itemAnimation: ItemAnimation? = null

    open val diffItemCallback: DiffUtil.ItemCallback<ITEM> =
        object : DiffUtil.ItemCallback<ITEM>() {

            override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
                return true
            }

        }

    fun setOnItemClickListener(listener: (holder: ItemViewHolder, item: ITEM) -> Unit) {
        itemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (holder: ItemViewHolder, item: ITEM) -> Boolean) {
        itemLongClickListener = listener
    }

    fun bindToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.adapter = this
    }

    fun <DELEGATE : ItemViewDelegate<ITEM, VB>> addItemViewDelegate(
        viewType: Int,
        delegate: DELEGATE
    ) {
        itemDelegates[viewType] = delegate
    }

    fun addItemViewDelegate(delegate: ItemViewDelegate<ITEM, VB>) {
        itemDelegates[itemDelegates.size] = delegate
    }

    fun addItemViewDelegates(vararg delegates: ItemViewDelegate<ITEM, VB>) {
        delegates.forEach {
            addItemViewDelegate(it)
        }
    }

    fun addItemViewDelegates(vararg delegates: Pair<Int, ItemViewDelegate<ITEM, VB>>) =
        delegates.forEach {
            addItemViewDelegate(it.first, it.second)
        }

    fun addHeaderView(header: ((parent: ViewGroup) -> ViewBinding)) {
        synchronized(lock) {
            val index = headerItems.size()
            headerItems.put(TYPE_HEADER_VIEW + headerItems.size(), header)
            notifyItemInserted(index)
        }
    }

    fun addFooterView(footer: ((parent: ViewGroup) -> ViewBinding)) =
        synchronized(lock) {
            val index = getActualItemCount() + footerItems.size()
            footerItems.put(TYPE_FOOTER_VIEW + footerItems.size(), footer)
            notifyItemInserted(index)
        }


    fun removeHeaderView(header: ((parent: ViewGroup) -> ViewBinding)) =
        synchronized(lock) {
            val index = headerItems.indexOfValue(header)
            if (index >= 0) {
                headerItems.remove(index)
                notifyItemRemoved(index)
            }
        }

    fun removeFooterView(footer: ((parent: ViewGroup) -> ViewBinding)) =
        synchronized(lock) {
            val index = footerItems.indexOfValue(footer)
            if (index >= 0) {
                footerItems.remove(index)
                notifyItemRemoved(getActualItemCount() + index - 2)
            }
        }

    fun setItems(items: List<ITEM>?) {
        synchronized(lock) {
            asyncListDiffer.submitList(items)
        }
    }

    fun setItem(position: Int, item: ITEM) {
        synchronized(lock) {
            val list = ArrayList(asyncListDiffer.currentList)
            list[position] = item
            asyncListDiffer.submitList(list)
        }
    }

    fun addItem(item: ITEM) {
        synchronized(lock) {
            val list = ArrayList(asyncListDiffer.currentList)
            list.add(item)
            asyncListDiffer.submitList(list)
        }
    }

    fun addItems(position: Int, newItems: List<ITEM>) {
        synchronized(lock) {
            val list = ArrayList(asyncListDiffer.currentList)
            list.addAll(position, newItems)
            asyncListDiffer.submitList(list)
        }
    }

    fun addItems(newItems: List<ITEM>) {
        synchronized(lock) {
            val list = ArrayList(asyncListDiffer.currentList)
            list.addAll(newItems)
            asyncListDiffer.submitList(list)
        }
    }

    fun removeItem(position: Int) {
        synchronized(lock) {
            val list = ArrayList(asyncListDiffer.currentList)
            if (list.removeAt(position) != null) {
                asyncListDiffer.submitList(list)
            }
        }
    }

    fun removeItem(item: ITEM) {
        synchronized(lock) {
            val list = ArrayList(asyncListDiffer.currentList)
            if (list.remove(item)) {
                asyncListDiffer.submitList(list)
            }
        }
    }

    fun removeItems(items: List<ITEM>) {
        synchronized(lock) {
            val list = ArrayList(asyncListDiffer.currentList)
            if (list.removeAll(items)) {
                asyncListDiffer.submitList(list)
            }
        }
    }

    fun swapItem(oldPosition: Int, newPosition: Int) {
        synchronized(lock) {
            val size = getActualItemCount()
            if (oldPosition in 0 until size && newPosition in 0 until size) {
                val srcPosition = oldPosition + getHeaderCount()
                val targetPosition = newPosition + getHeaderCount()
                Collections.swap(asyncListDiffer.currentList, srcPosition, targetPosition)
                notifyItemChanged(srcPosition)
                notifyItemChanged(targetPosition)
            }
        }
    }

    fun updateItem(item: ITEM) =
        synchronized(lock) {
            val index = asyncListDiffer.currentList.indexOf(item)
            if (index >= 0) {
                asyncListDiffer.currentList[index] = item
                notifyItemChanged(index)
            }
        }

    fun updateItem(position: Int, payload: Any) =
        synchronized(lock) {
            val size = getActualItemCount()
            if (position in 0 until size) {
                notifyItemChanged(position + getHeaderCount(), payload)
            }
        }

    fun updateItems(fromPosition: Int, toPosition: Int, payloads: Any) =
        synchronized(lock) {
            val size = getActualItemCount()
            if (fromPosition in 0 until size && toPosition in 0 until size) {
                notifyItemRangeChanged(
                    fromPosition + getHeaderCount(),
                    toPosition - fromPosition + 1,
                    payloads
                )
            }
        }

    fun clearItems() =
        synchronized(lock) {
            asyncListDiffer.submitList(arrayListOf())
        }

    fun isEmpty() = asyncListDiffer.currentList.isEmpty()

    fun isNotEmpty() = asyncListDiffer.currentList.isNotEmpty()

    /**
     * 除去header和footer
     */
    fun getActualItemCount() = asyncListDiffer.currentList.size


    fun getHeaderCount() = headerItems.size()


    fun getFooterCount() = footerItems.size()

    fun getItem(position: Int): ITEM? = asyncListDiffer.currentList.getOrNull(position)

    fun getItemByLayoutPosition(position: Int) =
        asyncListDiffer.currentList.getOrNull(position - getHeaderCount())

    fun getItems(): List<ITEM> = asyncListDiffer.currentList

    protected open fun getItemViewType(item: ITEM, position: Int) = 0

    /**
     * grid 模式下使用
     */
    protected open fun getSpanSize(viewType: Int, position: Int) = 1

    final override fun getItemCount() = getActualItemCount() + getHeaderCount() + getFooterCount()

    final override fun getItemViewType(position: Int) = when {
        isHeader(position) -> TYPE_HEADER_VIEW + position
        isFooter(position) -> TYPE_FOOTER_VIEW + position - getActualItemCount() - getHeaderCount()
        else -> getItem(getActualPosition(position))?.let {
            getItemViewType(it, getActualPosition(position))
        } ?: 0
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
            itemDelegates.getValue(viewType)
                .registerListener(holder, (holder.binding as VB))

            if (itemClickListener != null) {
                holder.itemView.setOnClickListener {
                    getItem(holder.layoutPosition)?.let {
                        itemClickListener?.invoke(holder, it)
                    }
                }
            }

            if (itemLongClickListener != null) {
                holder.itemView.setOnLongClickListener {
                    getItem(holder.layoutPosition)?.let {
                        itemLongClickListener?.invoke(holder, it) ?: true
                    } ?: true
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
            getItem(holder.layoutPosition - getHeaderCount())?.let {
                itemDelegates.getValue(getItemViewType(holder.layoutPosition))
                    .convert(holder, (holder.binding as VB), it, payloads)
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

    companion object {
        private const val TYPE_HEADER_VIEW = Int.MIN_VALUE
        private const val TYPE_FOOTER_VIEW = Int.MAX_VALUE - 999
    }

}




