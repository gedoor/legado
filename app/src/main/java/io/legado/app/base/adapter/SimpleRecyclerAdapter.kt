package io.legado.app.base.adapter

import android.content.Context
import androidx.viewbinding.ViewBinding

/**
 * Created by Invincible on 2017/12/15.
 */
abstract class SimpleRecyclerAdapter<ITEM, VB : ViewBinding>(context: Context) :
    CommonRecyclerAdapter<ITEM, VB>(context) {

    init {
        addItemViewDelegate(object : ItemViewDelegate<ITEM, VB>(context) {

            override fun convert(
                holder: ItemViewHolder,
                binding: VB,
                item: ITEM,
                payloads: MutableList<Any>
            ) {
                this@SimpleRecyclerAdapter.convert(holder, binding, item, payloads)
            }

            override fun registerListener(holder: ItemViewHolder, binding: VB) {
                this@SimpleRecyclerAdapter.registerListener(holder, binding)
            }
        })
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
}