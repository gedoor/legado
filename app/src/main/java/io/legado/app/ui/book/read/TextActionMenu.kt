package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import io.legado.app.R


class TextActionMenu(context: Context, callBack: CallBack) :
    PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    init {
        @SuppressLint("InflateParams")
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_action_menu, null)

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = false
    }


    interface CallBack {

    }
}