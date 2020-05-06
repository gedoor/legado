package io.legado.app.ui.book.read.config

import android.app.Dialog
import android.content.Context
import android.view.KeyEvent
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.hideSoftInput
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.removePref
import kotlinx.android.synthetic.main.dialog_page_key.*
import org.jetbrains.anko.sdk27.listeners.onClick


class PageKeyDialog(context: Context) : Dialog(context, R.style.AppTheme_AlertDialog) {

    init {
        setContentView(R.layout.dialog_page_key)
        et_prev.setText(context.getPrefInt(PreferKey.prevKey).toString())
        et_next.setText(context.getPrefInt(PreferKey.nextKey).toString())
        tv_ok.onClick {
            val prevKey = et_prev.text?.toString()
            if (prevKey.isNullOrEmpty()) {
                context.removePref(PreferKey.prevKey)
            } else {
                context.putPrefInt(PreferKey.prevKey, prevKey.toInt())
            }
            val nextKey = et_next.text?.toString()
            if (nextKey.isNullOrEmpty()) {
                context.removePref(PreferKey.nextKey)
            } else {
                context.putPrefInt(PreferKey.nextKey, nextKey.toInt())
            }
            dismiss()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            if (et_prev.hasFocus()) {
                et_prev.setText(keyCode.toString())
            } else if (et_next.hasFocus()) {
                et_next.setText(keyCode.toString())
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun dismiss() {
        super.dismiss()
        currentFocus?.hideSoftInput()
    }

}