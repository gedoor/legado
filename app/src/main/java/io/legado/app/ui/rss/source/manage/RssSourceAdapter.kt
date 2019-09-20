package io.legado.app.ui.rss.source.manage

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssSource
import io.legado.app.help.ItemTouchCallback

class RssSourceAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<RssSource>(context, R.layout.item_rss_source),
    ItemTouchCallback.OnItemTouchCallbackListener {

    override fun convert(holder: ItemViewHolder, item: RssSource, payloads: MutableList<Any>) {

    }


    override fun onSwiped(adapterPosition: Int) {

    }

    override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.customOrder == targetItem.customOrder) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.customOrder
                srcItem.customOrder = targetItem.customOrder
                targetItem.customOrder = srcOrder
                callBack.update(srcItem, targetItem)
            }
        }
        return true
    }

    interface CallBack {
        fun del(source: RssSource)
        fun edit(source: RssSource)
        fun update(vararg source: RssSource)
        fun toTop(source: RssSource)
        fun upOrder()
    }
}
