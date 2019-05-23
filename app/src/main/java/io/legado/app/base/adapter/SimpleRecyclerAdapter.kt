package io.legado.app.base.adapter

import android.content.Context

/**
 * Created by Invincible on 2017/12/15.
 */
abstract class SimpleRecyclerAdapter<ITEM>(context: Context) : CommonRecyclerAdapter<ITEM>(context) {

    init {
        addItemViewDelegate(object : ItemViewDelegate<ITEM>(context) {
            override val layoutID: Int
                get() = this@SimpleRecyclerAdapter.layoutID

            override fun convert(holder: ItemViewHolder, item: ITEM, payloads: MutableList<Any>) {
                this@SimpleRecyclerAdapter.convert(holder, item, payloads)
            }

        })

    }

    abstract val layoutID: Int

    abstract fun convert(holder: ItemViewHolder, item: ITEM, payloads: MutableList<Any>)
}