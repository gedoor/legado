package io.legado.app.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import kotlinx.android.synthetic.main.item_text.view.*
import kotlinx.android.synthetic.main.popup_keyboard_tool.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class KeyboardToolPop(
    context: Context,
    private val chars: List<String>,
    val onClickListener: OnClickListener?
) : PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    init {
        @SuppressLint("InflateParams")
        this.contentView = LayoutInflater.from(context).inflate(R.layout.popup_keyboard_tool, null)

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = false
        inputMethodMode = INPUT_METHOD_NEEDED //解决遮盖输入法
        initRecyclerView()
    }

    private fun initRecyclerView() = with(contentView) {
        val adapter = Adapter(context)
        recycler_view.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recycler_view.adapter = adapter
        adapter.setItems(chars)
    }

    inner class Adapter(context: Context) :
        SimpleRecyclerAdapter<String>(context, R.layout.item_text) {

        override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
            holder.itemView.text_view.text = item
            holder.itemView.onClick { onClickListener?.click(item) }
        }
    }

    interface OnClickListener {
        fun click(text: String)
    }

}
