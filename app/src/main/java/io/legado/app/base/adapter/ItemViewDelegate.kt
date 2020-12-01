package io.legado.app.base.adapter

import android.content.Context
import androidx.viewbinding.ViewBinding

/**
 * Created by Invincible on 2017/11/24.
 *
 * item代理，
 */
abstract class ItemViewDelegate<ITEM, VB : ViewBinding>(protected val context: Context) {

    /**
     * 如果使用了事件回调,回调里不要直接使用item,会出现不更新的问题,
     * 使用getItem(holder.layoutPosition)来获取item,
     * 或者使用registerListener(holder: ItemViewHolder, position: Int)
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