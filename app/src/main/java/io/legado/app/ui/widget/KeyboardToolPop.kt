package io.legado.app.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.constant.AppConst
import io.legado.app.databinding.ItemFilletTextBinding
import io.legado.app.databinding.PopupKeyboardToolBinding
import splitties.systemservices.layoutInflater

/**
 * 键盘帮助浮窗
 */
class KeyboardToolPop(
    context: Context,
    private val callBack: CallBack
) : PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    private val helpChar = "❓"

    private val binding = PopupKeyboardToolBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = false
        inputMethodMode = INPUT_METHOD_NEEDED //解决遮盖输入法
        initRecyclerView()
    }

    private fun initRecyclerView() = with(contentView) {
        val adapter = Adapter(context)
        binding.recyclerView.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        binding.recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemFilletTextBinding.inflate(layoutInflater, it, false).apply {
                textView.text = helpChar
                root.setOnClickListener {
                    callBack.keyboardHelp()
                }
            }
        }
        adapter.setItems(AppConst.charsets)
    }

    inner class Adapter(context: Context) :
        RecyclerAdapter<String, ItemFilletTextBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemFilletTextBinding {
            return ItemFilletTextBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemFilletTextBinding,
            item: String,
            payloads: MutableList<Any>
        ) {
            binding.run {
                textView.text = item
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemFilletTextBinding) {
            holder.itemView.apply {
                setOnClickListener {
                    getItemByLayoutPosition(holder.layoutPosition)?.let {
                        callBack.sendText(it)
                    }
                }
            }
        }
    }

    interface CallBack {

        fun keyboardHelp()

        fun sendText(text: String)

    }

}
