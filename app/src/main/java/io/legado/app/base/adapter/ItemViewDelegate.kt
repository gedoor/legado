package io.legado.app.base.adapter

import android.content.Context

/**
 * Created by Invincible on 2017/11/24.
 *
 * item代理，
 */
abstract class ItemViewDelegate<in ITEM>(protected val context: Context)  {

    abstract val layoutID: Int

    abstract fun convert(holder: ItemViewHolder, item: ITEM, payloads: MutableList<Any>)

}