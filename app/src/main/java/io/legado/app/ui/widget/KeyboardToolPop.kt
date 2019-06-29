package io.legado.app.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import io.legado.app.R


class KeyboardToolPop(context: Context, onClickListener: OnClickListener?) : PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    init {
        @SuppressLint("InflateParams")
        val view = LayoutInflater.from(context).inflate(R.layout.pop_keyboard_tool, null)
        this.contentView = view

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = false
        inputMethodMode = INPUT_METHOD_NEEDED //解决遮盖输入法

        val linearLayout = contentView.findViewById<LinearLayout>(R.id.ll_content)

        for (i in 0 until linearLayout.childCount) {
            val tv = linearLayout.getChildAt(i) as TextView
            tv.setOnClickListener { v ->
                (v as? TextView)?.text.toString().let {
                    onClickListener?.click(it)
                }
            }
        }
    }

    interface OnClickListener {
        fun click(text: String)
    }

}
