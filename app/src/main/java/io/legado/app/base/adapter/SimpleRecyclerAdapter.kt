package io.legado.app.base.adapter

import android.content.Context

/**
 * Created by Invincible on 2017/12/15.
 */
abstract class SimpleRecyclerAdapter<ITEM>(context: Context, private val layoutId: Int) :
    CommonRecyclerAdapter<ITEM>(context) {

    init {
        addItemViewDelegate(object : ItemViewDelegate<ITEM>(context, layoutId) {

            override fun convert(holder: ItemViewHolder, item: ITEM, payloads: MutableList<Any>) {
                this@SimpleRecyclerAdapter.convert(holder, item, payloads)
            }

            override fun registerListener(holder: ItemViewHolder) {
                this@SimpleRecyclerAdapter.registerListener(holder)
            }
        })
    }

    /**
     * 如果使用了事件回调,回调里不要直接使用item,会出现不更新的问题,
     * 使用getItem(holder.layoutPosition)来获取item
     */
    abstract fun convert(holder: ItemViewHolder, item: ITEM, payloads: MutableList<Any>)

    /**
     * 注册事件
     */
    abstract fun registerListener(holder: ItemViewHolder)
}